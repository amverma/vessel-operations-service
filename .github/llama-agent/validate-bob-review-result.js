import fs from 'node:fs';
import path from 'node:path';

const REQUIRED_TOP_LEVEL_KEYS = [
  'status',
  'confidence',
  'summary',
  'findings',
  'positives',
  'block_merge'
];

function parseArgs(argv) {
  const args = {
    jsonResult: '.github/artifacts/bob-review.json',
    markdownResult: '.github/artifacts/bob-review.md'
  };

  for (let i = 2; i < argv.length; i += 1) {
    const key = argv[i];
    const value = argv[i + 1];
    if (!value) {
      continue;
    }

    if (key === '--json-result') {
      args.jsonResult = value;
      i += 1;
    } else if (key === '--markdown-result') {
      args.markdownResult = value;
      i += 1;
    }
  }

  return args;
}

function fail(message, code) {
  process.stdout.write(`${message}\n`);
  process.exit(code);
}

function main() {
  const args = parseArgs(process.argv);
  const jsonPath = path.resolve(args.jsonResult);
  const markdownPath = path.resolve(args.markdownResult);

  if (!fs.existsSync(jsonPath)) {
    fail('Missing Bob JSON result', 2);
  }

  if (!fs.existsSync(markdownPath)) {
    fail('Missing Bob Markdown review', 2);
  }

  const data = JSON.parse(fs.readFileSync(jsonPath, 'utf8'));
  const missing = REQUIRED_TOP_LEVEL_KEYS.filter((key) => !(key in data));

  if (missing.length > 0) {
    fail(`Bob JSON result is missing required keys: ${missing.join(', ')}`, 2);
  }

  if (!Array.isArray(data.findings)) {
    fail(`Bob JSON result field 'findings' must be a list`, 2);
  }

  if (!Array.isArray(data.positives)) {
    fail(`Bob JSON result field 'positives' must be a list`, 2);
  }

  if (typeof data.block_merge !== 'boolean') {
    fail(`Bob JSON result field 'block_merge' must be boolean`, 2);
  }

  const status = String(data.status).toUpperCase();
  if (!['PASS', 'FAIL', 'NEEDS_ATTENTION'].includes(status)) {
    fail(`Bob JSON result field 'status' must be PASS, FAIL, or NEEDS_ATTENTION`, 2);
  }

  process.stdout.write('Bob review artifacts are structurally valid\n');

  if (data.block_merge) {
    process.stdout.write('Bob decided to block merge\n');
    process.exit(1);
  }

  process.stdout.write('Bob approved or did not block merge\n');
}

main();

// Made with Bob
