import fs from 'node:fs';
import path from 'node:path';

function parseArgs(argv) {
  const args = {
    repoRoot: '.',
    config: '.github/bob-agentic-config.yml',
    changedFiles: '.github/artifacts/changed-files-filtered.txt',
    diffFile: '.github/artifacts/pull-request.diff',
    outputDir: '.github/artifacts',
    repository: '',
    prNumber: '',
    prTitle: '',
    prAuthor: '',
    baseRef: '',
    headRef: ''
  };

  for (let i = 2; i < argv.length; i += 1) {
    const key = argv[i];
    const value = argv[i + 1];

    if (!value) {
      continue;
    }

    switch (key) {
      case '--repo-root':
        args.repoRoot = value;
        i += 1;
        break;
      case '--config':
        args.config = value;
        i += 1;
        break;
      case '--changed-files':
        args.changedFiles = value;
        i += 1;
        break;
      case '--diff-file':
        args.diffFile = value;
        i += 1;
        break;
      case '--output-dir':
        args.outputDir = value;
        i += 1;
        break;
      case '--repository':
        args.repository = value;
        i += 1;
        break;
      case '--pr-number':
        args.prNumber = value;
        i += 1;
        break;
      case '--pr-title':
        args.prTitle = value;
        i += 1;
        break;
      case '--pr-author':
        args.prAuthor = value;
        i += 1;
        break;
      case '--base-ref':
        args.baseRef = value;
        i += 1;
        break;
      case '--head-ref':
        args.headRef = value;
        i += 1;
        break;
      default:
        break;
    }
  }

  return args;
}

function readText(filePath) {
  if (!fs.existsSync(filePath)) {
    return '';
  }
  return fs.readFileSync(filePath, 'utf8');
}

function splitLines(value) {
  return String(value)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

function parseSimpleYaml(content) {
  const result = {};
  const stack = [{ indent: -1, target: result }];

  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.replace(/\t/g, '    ');
    if (!line.trim() || line.trim().startsWith('#')) {
      continue;
    }

    const indent = line.match(/^ */)[0].length;
    const trimmed = line.trim();

    while (stack.length > 1 && indent <= stack[stack.length - 1].indent) {
      stack.pop();
    }

    const currentTarget = stack[stack.length - 1].target;

    if (trimmed.startsWith('- ')) {
      const value = trimmed.slice(2).trim().replace(/^['"]|['"]$/g, '');
      if (!Array.isArray(currentTarget.__list__)) {
        currentTarget.__list__ = [];
      }
      currentTarget.__list__.push(value);
      continue;
    }

    const separatorIndex = trimmed.indexOf(':');
    if (separatorIndex === -1) {
      continue;
    }

    const key = trimmed.slice(0, separatorIndex).trim();
    const value = trimmed.slice(separatorIndex + 1).trim();

    if (!value) {
      currentTarget[key] = {};
      stack.push({ indent, target: currentTarget[key] });
    } else {
      currentTarget[key] = value.replace(/^['"]|['"]$/g, '');
    }
  }

  function normalize(node) {
    if (node && typeof node === 'object' && !Array.isArray(node)) {
      const keys = Object.keys(node);
      if (keys.length === 1 && keys[0] === '__list__') {
        return node.__list__;
      }

      for (const key of keys) {
        node[key] = normalize(node[key]);
      }
    }
    return node;
  }

  return normalize(result);
}

function collectPrompts(configPath, config) {
  const configDir = path.dirname(configPath);
  const bobConfig = config.bob || {};

  return {
    main: readText(path.resolve(configDir, bobConfig.prompt_main || '')),
    critical: readText(path.resolve(configDir, bobConfig.prompt_critical || '')),
    autofix: readText(path.resolve(configDir, bobConfig.prompt_autofix || ''))
  };
}

function collectPolicyDocs(repoRoot) {
  const files = [
    'ARCHITECTURE.md',
    'DISASTER_RECOVERY.md',
    'RESILIENCE_COMPLIANCE_REPORT.md',
    '.github/README-RESILIENCE-CHECKER.md'
  ];

  const docs = {};
  for (const relativePath of files) {
    const absolutePath = path.join(repoRoot, relativePath);
    if (fs.existsSync(absolutePath)) {
      docs[relativePath] = readText(absolutePath);
    }
  }
  return docs;
}

function main() {
  const args = parseArgs(process.argv);
  const repoRoot = path.resolve(args.repoRoot);
  const configPath = path.resolve(args.config);
  const outputDir = path.resolve(args.outputDir);
  const changedFilesPath = path.resolve(args.changedFiles);
  const diffFilePath = path.resolve(args.diffFile);

  fs.mkdirSync(outputDir, { recursive: true });

  const config = parseSimpleYaml(readText(configPath));
  const changedFiles = splitLines(readText(changedFilesPath));
  const diff = readText(diffFilePath);
  const maxDiffChars = Number(config.inputs?.max_diff_chars || 150000);

  const bundle = {
    repository: args.repository,
    pull_request: {
      number: args.prNumber,
      title: args.prTitle,
      author: args.prAuthor,
      base_ref: args.baseRef,
      head_ref: args.headRef
    },
    changed_files: changedFiles,
    diff: diff.slice(0, maxDiffChars),
    policy_docs: collectPolicyDocs(repoRoot),
    prompts: collectPrompts(configPath, config),
    config,
    supporting_v1: {
      executed: false,
      reason: 'Node/Ollama mode does not execute Python supporting context locally'
    },
    instruction: 'Llama via Ollama must produce the authoritative review result. Local scripts must not determine block_merge.'
  };

  const outputFile = path.join(outputDir, 'bob-pr-review-bundle.json');
  fs.writeFileSync(outputFile, `${JSON.stringify(bundle, null, 2)}\n`, 'utf8');

  process.stdout.write(`${JSON.stringify({
    bundle_file: outputFile,
    changed_file_count: changedFiles.length
  }, null, 2)}\n`);
}

main();

// Made with Bob
