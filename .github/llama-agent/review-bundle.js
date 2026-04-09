import fs from 'node:fs';
import path from 'node:path';

const DEFAULT_OLLAMA_URL = process.env.OLLAMA_BASE_URL || 'http://127.0.0.1:11434';
const DEFAULT_MODEL = process.env.OLLAMA_MODEL || 'llama3.1:8b';

function readJson(filePath) {
  return JSON.parse(fs.readFileSync(filePath, 'utf8'));
}

function writeText(filePath, content) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
  fs.writeFileSync(filePath, content, 'utf8');
}

function parseArgs(argv) {
  const args = {
    bundle: '.github/artifacts/bob-pr-review-bundle.json',
    markdownOutput: '.github/artifacts/bob-review.md',
    jsonOutput: '.github/artifacts/bob-review.json'
  };

  for (let i = 2; i < argv.length; i += 1) {
    const current = argv[i];
    const next = argv[i + 1];

    if (current === '--bundle' && next) {
      args.bundle = next;
      i += 1;
    } else if (current === '--markdown-output' && next) {
      args.markdownOutput = next;
      i += 1;
    } else if (current === '--json-output' && next) {
      args.jsonOutput = next;
      i += 1;
    }
  }

  return args;
}

function buildPrompt(bundle) {
  const changedFiles = (bundle.changed_files || []).slice(0, 200).join('\n');
  const policyDocs = Object.entries(bundle.policy_docs || {})
    .map(([name, content]) => `## Document: ${name}\n${String(content).slice(0, 12000)}`)
    .join('\n\n');

  const promptSections = [
    'You are Bob Resilience Guardian running through a local Ollama-hosted Llama model.',
    'Review this pull request for enterprise resilience patterns and operational safety.',
    'You must evaluate the PR against resilience expectations such as circuit breakers, retries, timeouts, bulkheads, rate limiting, health checks, graceful shutdown, idempotency, DLQ, structured logging, tracing, metrics, schema evolution, Kafka best practices, resource limits, autoscaling, secrets, and DR readiness.',
    'Return ONLY valid JSON with this exact top-level structure:',
    JSON.stringify({
      status: 'PASS',
      confidence: 'High',
      summary: 'Short summary',
      findings: [
        {
          id: 'R14',
          title: 'Example finding',
          severity: 'CRITICAL',
          files: ['src/example/File.java'],
          why_it_matters: 'Why this matters',
          recommendation: 'What to change',
          auto_fix_feasible: false
        }
      ],
      positives: ['Optional positive observation'],
      block_merge: false
    }, null, 2),
    'Rules:',
    '- status must be one of PASS, FAIL, NEEDS_ATTENTION',
    '- block_merge must be true only for credible merge-blocking resilience risk',
    '- findings and positives must be arrays',
    '- return JSON only, no markdown fences, no explanations before or after JSON',
    `Repository: ${bundle.repository || ''}`,
    `PR Number: ${bundle.pull_request?.number || ''}`,
    `PR Title: ${bundle.pull_request?.title || ''}`,
    `PR Author: ${bundle.pull_request?.author || ''}`,
    `Base Ref: ${bundle.pull_request?.base_ref || ''}`,
    `Head Ref: ${bundle.pull_request?.head_ref || ''}`,
    'Changed Files:',
    changedFiles || '(none)',
    'Diff:',
    String(bundle.diff || '').slice(0, 80000),
    'Main Prompt:',
    bundle.prompts?.main || '',
    'Critical Prompt:',
    bundle.prompts?.critical || '',
    'Autofix Guardrails:',
    bundle.prompts?.autofix || '',
    'Policy Documents:',
    policyDocs || '(none)'
  ];

  return promptSections.join('\n\n');
}

async function callOllama(prompt) {
  const response = await fetch(`${DEFAULT_OLLAMA_URL}/api/generate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      model: DEFAULT_MODEL,
      prompt,
      stream: false,
      options: {
        temperature: 0.1
      }
    })
  });

  if (!response.ok) {
    throw new Error(`Ollama request failed: ${response.status} ${response.statusText}`);
  }

  const data = await response.json();
  return String(data.response || '').trim();
}

function normalizeResult(rawText) {
  const sanitized = rawText
    .replace(/^```json\s*/i, '')
    .replace(/^```\s*/i, '')
    .replace(/\s*```$/i, '')
    .trim();

  const parsed = JSON.parse(sanitized);

  return {
    status: String(parsed.status || 'NEEDS_ATTENTION').toUpperCase(),
    confidence: String(parsed.confidence || 'Medium'),
    summary: String(parsed.summary || 'Llama review completed.'),
    findings: Array.isArray(parsed.findings) ? parsed.findings : [],
    positives: Array.isArray(parsed.positives) ? parsed.positives : [],
    block_merge: Boolean(parsed.block_merge)
  };
}

function renderMarkdown(result) {
  const findingsSection = result.findings.length === 0
    ? '- No resilience findings were reported.'
    : result.findings.map((finding, index) => {
        const files = Array.isArray(finding.files) && finding.files.length > 0
          ? finding.files.map((file) => `  - ${file}`).join('\n')
          : '  - Not specified';

        return [
          `${index + 1}. **${finding.id || 'UNSPECIFIED'} - ${finding.title || 'Untitled finding'}**`,
          `   - Severity: ${finding.severity || 'UNKNOWN'}`,
          `   - Why it matters: ${finding.why_it_matters || 'Not provided'}`,
          `   - Recommendation: ${finding.recommendation || 'Not provided'}`,
          `   - Auto-fix feasible: ${String(finding.auto_fix_feasible ?? false)}`,
          '   - Files:',
          files
        ].join('\n');
      }).join('\n\n');

  const positivesSection = result.positives.length === 0
    ? '- No explicit positives were reported.'
    : result.positives.map((item) => `- ${item}`).join('\n');

  return [
    '# 🛡️ Bob Resilience Guardian Analysis v3.0',
    '',
    `**Status:** ${result.status}`,
    `**Confidence:** ${result.confidence}`,
    `**Block Merge:** ${result.block_merge}`,
    '',
    '## Summary',
    result.summary,
    '',
    '## Findings',
    findingsSection,
    '',
    '## Positives',
    positivesSection,
    '',
    '## Overall Assessment',
    result.block_merge
      ? 'Merge should be blocked until the identified resilience risks are addressed.'
      : 'No merge-blocking resilience risk was identified by the agent.',
    ''
  ].join('\n');
}

async function main() {
  const args = parseArgs(process.argv);
  const bundle = readJson(args.bundle);
  const prompt = buildPrompt(bundle);
  const raw = await callOllama(prompt);
  const result = normalizeResult(raw);
  const markdown = renderMarkdown(result);

  writeText(args.jsonOutput, `${JSON.stringify(result, null, 2)}\n`);
  writeText(args.markdownOutput, `${markdown}\n`);

  process.stdout.write(`${JSON.stringify({
    model: DEFAULT_MODEL,
    ollama_url: DEFAULT_OLLAMA_URL,
    json_output: args.jsonOutput,
    markdown_output: args.markdownOutput,
    block_merge: result.block_merge
  }, null, 2)}\n`);
}

main().catch((error) => {
  process.stderr.write(`${error.stack || error.message}\n`);
  process.exit(1);
});

// Made with Bob
