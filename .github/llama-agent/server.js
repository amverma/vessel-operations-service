import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';

const PORT = Number(process.env.LLAMA_AGENT_PORT || 8787);
const HOST = process.env.LLAMA_AGENT_HOST || '127.0.0.1';
const REPO_ROOT = process.env.LLAMA_AGENT_REPO_ROOT || process.cwd();
const JOBS_ROOT = path.join(REPO_ROOT, '.github', 'artifacts', 'jobs');
const jobs = new Map();
const pendingQueue = [];
let activeJobId = null;

function sendJson(response, statusCode, body) {
  response.writeHead(statusCode, { 'Content-Type': 'application/json' });
  response.end(`${JSON.stringify(body, null, 2)}\n`);
}

function sendError(response, statusCode, error, details = {}) {
  sendJson(response, statusCode, {
    error,
    ...details
  });
}

function collectRequestBody(request) {
  return new Promise((resolve, reject) => {
    let raw = '';
    request.on('data', (chunk) => {
      raw += chunk.toString('utf8');
    });
    request.on('end', () => resolve(raw));
    request.on('error', reject);
  });
}

function ensureDir(dirPath) {
  fs.mkdirSync(dirPath, { recursive: true });
}

function writeJson(filePath, data) {
  ensureDir(path.dirname(filePath));
  fs.writeFileSync(filePath, `${JSON.stringify(data, null, 2)}\n`, 'utf8');
}

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function readJsonIfExists(filePath) {
  if (!fs.existsSync(filePath)) {
    return null;
  }

  return readJson(filePath);
}

function getJobDir(jobId) {
  return path.join(JOBS_ROOT, jobId);
}

function getJobMetaPath(jobId) {
  return path.join(getJobDir(jobId), 'job.json');
}

function getJobBundlePath(jobId) {
  return path.join(getJobDir(jobId), 'bob-pr-review-bundle.json');
}

function getJobMarkdownPath(jobId) {
  return path.join(getJobDir(jobId), 'bob-review.md');
}

function getJobJsonPath(jobId) {
  return path.join(getJobDir(jobId), 'bob-review.json');
}

function createArtifactPaths(jobId) {
  return {
    artifactsDir: getJobDir(jobId),
    bundlePath: getJobBundlePath(jobId),
    markdownPath: getJobMarkdownPath(jobId),
    jsonPath: getJobJsonPath(jobId)
  };
}

function writeBundleArtifacts(bundle, jobId) {
  const artifactPaths = createArtifactPaths(jobId);
  ensureDir(artifactPaths.artifactsDir);
  fs.writeFileSync(artifactPaths.bundlePath, `${JSON.stringify(bundle, null, 2)}\n`, 'utf8');
  return artifactPaths;
}

function mirrorJobArtifactsToDefaultPaths(artifactPaths) {
  const defaultArtifactsDir = path.join(REPO_ROOT, '.github', 'artifacts');
  ensureDir(defaultArtifactsDir);

  if (fs.existsSync(artifactPaths.bundlePath)) {
    fs.copyFileSync(artifactPaths.bundlePath, path.join(defaultArtifactsDir, 'bob-pr-review-bundle.json'));
  }

  if (fs.existsSync(artifactPaths.jsonPath)) {
    fs.copyFileSync(artifactPaths.jsonPath, path.join(defaultArtifactsDir, 'bob-review.json'));
  }

  if (fs.existsSync(artifactPaths.markdownPath)) {
    fs.copyFileSync(artifactPaths.markdownPath, path.join(defaultArtifactsDir, 'bob-review.md'));
  }
}

function getJobResponse(job) {
  return {
    job_id: job.id,
    status: job.status,
    created_at: job.created_at,
    updated_at: job.updated_at,
    files: job.files,
    queue_position: job.status === 'queued' ? pendingQueue.indexOf(job.id) + 1 : 0,
    review_json: job.status === 'completed' ? job.review_json : undefined,
    review_markdown: job.status === 'completed' ? job.review_markdown : undefined,
    error: job.status === 'failed' ? job.error : undefined
  };
}

function persistJob(job) {
  writeJson(getJobMetaPath(job.id), getJobResponse(job));
}

function hydrateJobsFromDisk() {
  ensureDir(JOBS_ROOT);
  const entries = fs.readdirSync(JOBS_ROOT, { withFileTypes: true });

  for (const entry of entries) {
    if (!entry.isDirectory()) {
      continue;
    }

    const saved = readJsonIfExists(getJobMetaPath(entry.name));
    if (!saved || !saved.job_id) {
      continue;
    }

    jobs.set(saved.job_id, {
      id: saved.job_id,
      status: saved.status || 'failed',
      created_at: saved.created_at || new Date().toISOString(),
      updated_at: saved.updated_at || new Date().toISOString(),
      files: saved.files || {
        bundle: getJobBundlePath(saved.job_id),
        json: getJobJsonPath(saved.job_id),
        markdown: getJobMarkdownPath(saved.job_id)
      },
      review_json: saved.review_json || null,
      review_markdown: saved.review_markdown || null,
      error: saved.error || null
    });
  }
}

function runReviewer(artifactPaths) {
  return new Promise((resolve, reject) => {
    const child = spawn(
      process.execPath,
      [
        path.join(REPO_ROOT, '.github', 'llama-agent', 'review-bundle.js'),
        '--bundle',
        artifactPaths.bundlePath,
        '--markdown-output',
        artifactPaths.markdownPath,
        '--json-output',
        artifactPaths.jsonPath
      ],
      {
        cwd: REPO_ROOT,
        stdio: ['ignore', 'pipe', 'pipe'],
        env: process.env
      }
    );

    let stdout = '';
    let stderr = '';

    child.stdout.on('data', (chunk) => {
      stdout += chunk.toString('utf8');
    });

    child.stderr.on('data', (chunk) => {
      stderr += chunk.toString('utf8');
    });

    child.on('error', reject);

    child.on('close', (code) => {
      if (code !== 0) {
        reject(new Error(`Reviewer process failed with exit code ${code}\n${stderr}`));
        return;
      }

      const reviewJson = readJson(artifactPaths.jsonPath);
      const reviewMarkdown = fs.readFileSync(artifactPaths.markdownPath, 'utf8');

      resolve({
        stdout,
        stderr,
        reviewJson,
        reviewMarkdown,
        files: {
          bundle: artifactPaths.bundlePath,
          json: artifactPaths.jsonPath,
          markdown: artifactPaths.markdownPath
        }
      });
    });
  });
}

async function processNextJob() {
  if (activeJobId || pendingQueue.length === 0) {
    return;
  }

  const jobId = pendingQueue.shift();
  const job = jobs.get(jobId);
  if (!job) {
    setImmediate(processNextJob);
    return;
  }

  activeJobId = jobId;
  job.status = 'running';
  job.updated_at = new Date().toISOString();
  persistJob(job);

  try {
    const result = await runReviewer(job.files);
    mirrorJobArtifactsToDefaultPaths(job.files);
    job.status = 'completed';
    job.updated_at = new Date().toISOString();
    job.review_json = result.reviewJson;
    job.review_markdown = result.reviewMarkdown;
    job.files = result.files;
    persistJob(job);
  } catch (error) {
    job.status = 'failed';
    job.updated_at = new Date().toISOString();
    job.error = error.message || 'Unexpected reviewer failure';
    persistJob(job);
  } finally {
    activeJobId = null;
    setImmediate(processNextJob);
  }
}

function createJobFromBundle(bundle) {
  const jobId = crypto.randomUUID();
  const artifactPaths = writeBundleArtifacts(bundle, jobId);

  const job = {
    id: jobId,
    status: 'queued',
    created_at: new Date().toISOString(),
    updated_at: new Date().toISOString(),
    files: {
      bundle: artifactPaths.bundlePath,
      json: artifactPaths.jsonPath,
      markdown: artifactPaths.markdownPath
    },
    review_json: null,
    review_markdown: null,
    error: null
  };

  jobs.set(jobId, job);
  persistJob(job);
  pendingQueue.push(jobId);
  setImmediate(processNextJob);

  return job;
}

function createJobFromBundleFile(bundleFilePath) {
  const resolvedBundlePath = path.resolve(bundleFilePath);

  if (!fs.existsSync(resolvedBundlePath)) {
    throw new Error(`Bundle file does not exist: ${resolvedBundlePath}`);
  }

  const bundle = readJson(resolvedBundlePath);
  return createJobFromBundle(bundle);
}

hydrateJobsFromDisk();

const server = http.createServer(async (request, response) => {
  try {
    const url = new URL(request.url, `http://${request.headers.host || `${HOST}:${PORT}`}`);

    if (request.method === 'GET' && url.pathname === '/health') {
      sendJson(response, 200, {
        status: 'ok',
        service: 'llama-resilience-agent',
        host: HOST,
        port: PORT,
        active_job_id: activeJobId,
        queued_jobs: pendingQueue.length
      });
      return;
    }

    if (request.method === 'POST' && url.pathname === '/review') {
      const rawBody = await collectRequestBody(request);
      let payload = {};

      try {
        payload = rawBody ? JSON.parse(rawBody) : {};
      } catch (error) {
        sendError(response, 400, 'Invalid JSON payload', {
          details: error.message
        });
        return;
      }

      if (!payload.bundle || typeof payload.bundle !== 'object') {
        sendError(response, 400, 'Missing required object field: bundle');
        return;
      }

      try {
        const job = createJobFromBundle(payload.bundle);

        sendJson(response, 202, {
          status: 'accepted',
          job_id: job.id,
          poll_url: `/review/${job.id}`,
          files: job.files,
          queue_position: pendingQueue.indexOf(job.id) + 1
        });
      } catch (error) {
        sendError(response, 500, 'Failed to create review job from inline bundle', {
          details: error.message
        });
      }
      return;
    }

    if (request.method === 'POST' && url.pathname === '/review-file') {
      const rawBody = await collectRequestBody(request);
      let payload = {};

      try {
        payload = rawBody ? JSON.parse(rawBody) : {};
      } catch (error) {
        sendError(response, 400, 'Invalid JSON payload', {
          details: error.message
        });
        return;
      }

      if (!payload.bundle_file || typeof payload.bundle_file !== 'string') {
        sendError(response, 400, 'Missing required string field: bundle_file');
        return;
      }

      try {
        const job = createJobFromBundleFile(payload.bundle_file);

        sendJson(response, 202, {
          status: 'accepted',
          job_id: job.id,
          poll_url: `/review/${job.id}`,
          files: job.files,
          queue_position: pendingQueue.indexOf(job.id) + 1
        });
      } catch (error) {
        sendError(response, 500, 'Failed to create review job from bundle file', {
          details: error.message,
          bundle_file: payload.bundle_file
        });
      }
      return;
    }

    if (request.method === 'GET' && url.pathname.startsWith('/review/')) {
      const jobId = url.pathname.slice('/review/'.length);
      const job = jobs.get(jobId);

      if (!job) {
        sendJson(response, 404, {
          error: 'Unknown review job'
        });
        return;
      }

      sendJson(response, 200, getJobResponse(job));
      return;
    }

    sendJson(response, 404, {
      error: 'Not found'
    });
  } catch (error) {
    sendJson(response, 500, {
      error: error.message || 'Unexpected server error'
    });
  }
});

server.headersTimeout = 60_000;
server.requestTimeout = 60_000;
server.keepAliveTimeout = 5_000;

server.listen(PORT, HOST, () => {
  process.stdout.write(
    `${JSON.stringify({
      service: 'llama-resilience-agent',
      url: `http://${HOST}:${PORT}`,
      repo_root: REPO_ROOT,
      jobs_root: JOBS_ROOT
    }, null, 2)}\n`
  );
});

// Made with Bob
