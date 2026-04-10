import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';

const PORT = Number(process.env.LLAMA_AGENT_PORT || 8787);
const HOST = process.env.LLAMA_AGENT_HOST || '127.0.0.1';
const REPO_ROOT = process.env.LLAMA_AGENT_REPO_ROOT || process.cwd();
const jobs = new Map();

function sendJson(response, statusCode, body) {
  response.writeHead(statusCode, { 'Content-Type': 'application/json' });
  response.end(`${JSON.stringify(body, null, 2)}\n`);
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

function writeBundleArtifacts(rootDir, bundle, jobId) {
  const artifactsDir = path.join(rootDir, '.github', 'artifacts', 'jobs', jobId);
  fs.mkdirSync(artifactsDir, { recursive: true });

  const bundlePath = path.join(artifactsDir, 'bob-pr-review-bundle.json');
  const markdownPath = path.join(artifactsDir, 'bob-review.md');
  const jsonPath = path.join(artifactsDir, 'bob-review.json');

  fs.writeFileSync(bundlePath, `${JSON.stringify(bundle, null, 2)}\n`, 'utf8');

  return { artifactsDir, bundlePath, markdownPath, jsonPath };
}

function mirrorJobArtifactsToDefaultPaths(rootDir, artifactPaths) {
  const defaultArtifactsDir = path.join(rootDir, '.github', 'artifacts');
  fs.mkdirSync(defaultArtifactsDir, { recursive: true });

  fs.copyFileSync(artifactPaths.bundlePath, path.join(defaultArtifactsDir, 'bob-pr-review-bundle.json'));
  fs.copyFileSync(artifactPaths.jsonPath, path.join(defaultArtifactsDir, 'bob-review.json'));
  fs.copyFileSync(artifactPaths.markdownPath, path.join(defaultArtifactsDir, 'bob-review.md'));
}

function runReviewer(rootDir, artifactPaths) {
  return new Promise((resolve, reject) => {
    const child = spawn(
      process.execPath,
      [
        path.join(rootDir, '.github', 'llama-agent', 'review-bundle.js'),
        '--bundle',
        artifactPaths.bundlePath,
        '--markdown-output',
        artifactPaths.markdownPath,
        '--json-output',
        artifactPaths.jsonPath
      ],
      {
        cwd: rootDir,
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

      const reviewJson = JSON.parse(fs.readFileSync(artifactPaths.jsonPath, 'utf8'));
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

function createJob(bundle) {
  const jobId = crypto.randomUUID();
  const artifactPaths = writeBundleArtifacts(REPO_ROOT, bundle, jobId);

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

  runReviewer(REPO_ROOT, artifactPaths)
    .then((result) => {
      mirrorJobArtifactsToDefaultPaths(REPO_ROOT, artifactPaths);
      job.status = 'completed';
      job.updated_at = new Date().toISOString();
      job.review_json = result.reviewJson;
      job.review_markdown = result.reviewMarkdown;
      job.files = result.files;
    })
    .catch((error) => {
      job.status = 'failed';
      job.updated_at = new Date().toISOString();
      job.error = error.message || 'Unexpected reviewer failure';
    });

  return job;
}

function getJobResponse(job) {
  return {
    job_id: job.id,
    status: job.status,
    created_at: job.created_at,
    updated_at: job.updated_at,
    files: job.files,
    review_json: job.status === 'completed' ? job.review_json : undefined,
    review_markdown: job.status === 'completed' ? job.review_markdown : undefined,
    error: job.status === 'failed' ? job.error : undefined
  };
}

const server = http.createServer(async (request, response) => {
  try {
    const url = new URL(request.url, `http://${request.headers.host || `${HOST}:${PORT}`}`);

    if (request.method === 'GET' && url.pathname === '/health') {
      sendJson(response, 200, {
        status: 'ok',
        service: 'llama-resilience-agent',
        host: HOST,
        port: PORT
      });
      return;
    }

    if (request.method === 'POST' && url.pathname === '/review') {
      const rawBody = await collectRequestBody(request);
      const payload = rawBody ? JSON.parse(rawBody) : {};

      if (!payload.bundle || typeof payload.bundle !== 'object') {
        sendJson(response, 400, {
          error: 'Missing required object field: bundle'
        });
        return;
      }

      const job = createJob(payload.bundle);

      sendJson(response, 202, {
        status: 'accepted',
        job_id: job.id,
        poll_url: `/review/${job.id}`,
        files: job.files
      });
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

server.listen(PORT, HOST, () => {
  process.stdout.write(
    `${JSON.stringify({
      service: 'llama-resilience-agent',
      url: `http://${HOST}:${PORT}`,
      repo_root: REPO_ROOT
    }, null, 2)}\n`
  );
});

// Made with Bob
