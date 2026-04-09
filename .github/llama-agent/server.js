import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { spawn } from 'node:child_process';

const PORT = Number(process.env.LLAMA_AGENT_PORT || 8787);
const HOST = process.env.LLAMA_AGENT_HOST || '127.0.0.1';
const REPO_ROOT = process.env.LLAMA_AGENT_REPO_ROOT || process.cwd();

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

function writeBundleArtifacts(rootDir, bundle) {
  const artifactsDir = path.join(rootDir, '.github', 'artifacts');
  fs.mkdirSync(artifactsDir, { recursive: true });

  const bundlePath = path.join(artifactsDir, 'bob-pr-review-bundle.json');
  const markdownPath = path.join(artifactsDir, 'bob-review.md');
  const jsonPath = path.join(artifactsDir, 'bob-review.json');

  fs.writeFileSync(bundlePath, `${JSON.stringify(bundle, null, 2)}\n`, 'utf8');

  return { artifactsDir, bundlePath, markdownPath, jsonPath };
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

const server = http.createServer(async (request, response) => {
  try {
    if (request.method === 'GET' && request.url === '/health') {
      sendJson(response, 200, {
        status: 'ok',
        service: 'llama-resilience-agent',
        host: HOST,
        port: PORT
      });
      return;
    }

    if (request.method === 'POST' && request.url === '/review') {
      const rawBody = await collectRequestBody(request);
      const payload = rawBody ? JSON.parse(rawBody) : {};

      if (!payload.bundle || typeof payload.bundle !== 'object') {
        sendJson(response, 400, {
          error: 'Missing required object field: bundle'
        });
        return;
      }

      const artifactPaths = writeBundleArtifacts(REPO_ROOT, payload.bundle);
      const result = await runReviewer(REPO_ROOT, artifactPaths);

      sendJson(response, 200, {
        status: 'completed',
        review_json: result.reviewJson,
        review_markdown: result.reviewMarkdown,
        files: result.files
      });
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
