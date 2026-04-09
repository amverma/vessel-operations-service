#!/usr/bin/env python3
"""
Package pull request context for Bob Agentic AI review.

This script intentionally does not make the resilience decision.
Its job is only to collect context and prepare a bundle for Bob.
"""

from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path
from typing import Any


def read_text(path: Path) -> str:
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8")


def write_json(path: Path, data: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2), encoding="utf-8")


def load_yaml(path: Path) -> dict[str, Any]:
    import yaml  # type: ignore
    return yaml.safe_load(path.read_text(encoding="utf-8")) or {}


def run_command(command: list[str], cwd: Path) -> tuple[int, str, str]:
    result = subprocess.run(
        command,
        cwd=str(cwd),
        capture_output=True,
        text=True,
        shell=False,
    )
    return result.returncode, result.stdout, result.stderr


def split_lines(value: str) -> list[str]:
    return [line.strip() for line in value.splitlines() if line.strip()]


def maybe_collect_v1_report(repo_root: Path, artifact_dir: Path) -> dict[str, Any]:
    checker = repo_root / ".github" / "scripts" / "resilience_checker.py"
    output_file = artifact_dir / "v1-supporting-report.json"

    if not checker.exists():
        return {"executed": False, "reason": "V1 checker not found"}

    command = [
        sys.executable,
        str(checker),
        "--repo-path",
        ".",
        "--output-format",
        "json",
        "--output-file",
        str(output_file),
        "--severity-threshold",
        "CRITICAL",
    ]
    code, stdout, stderr = run_command(command, repo_root)
    return {
        "executed": output_file.exists(),
        "exit_code": code,
        "output_file": str(output_file) if output_file.exists() else "",
        "stdout": stdout,
        "stderr": stderr,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Prepare PR bundle for Bob")
    parser.add_argument("--repo-root", default=".")
    parser.add_argument("--config", required=True)
    parser.add_argument("--changed-files", required=True)
    parser.add_argument("--diff-file", required=True)
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--repository", default="")
    parser.add_argument("--pr-number", default="")
    parser.add_argument("--pr-title", default="")
    parser.add_argument("--pr-author", default="")
    parser.add_argument("--base-ref", default="")
    parser.add_argument("--head-ref", default="")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    config_path = Path(args.config).resolve()
    output_dir = Path(args.output_dir).resolve()
    changed_files_path = Path(args.changed_files).resolve()
    diff_file_path = Path(args.diff_file).resolve()

    output_dir.mkdir(parents=True, exist_ok=True)

    config = load_yaml(config_path)
    changed_files = split_lines(read_text(changed_files_path))
    diff_text = read_text(diff_file_path)

    prompts = {
        "main": read_text((config_path.parent / Path(config["bob"]["prompt_main"])).resolve()),
        "critical": read_text((config_path.parent / Path(config["bob"]["prompt_critical"])).resolve()),
        "autofix": read_text((config_path.parent / Path(config["bob"]["prompt_autofix"])).resolve()),
    }

    policy_docs = {}
    for path in [
        "ARCHITECTURE.md",
        "DISASTER_RECOVERY.md",
        "RESILIENCE_COMPLIANCE_REPORT.md",
        ".github/README-RESILIENCE-CHECKER.md",
    ]:
        file_path = repo_root / path
        if file_path.exists():
            policy_docs[path] = read_text(file_path)

    v1_context = {"executed": False}
    if config.get("inputs", {}).get("include_v1_as_supporting_context", False):
        v1_context = maybe_collect_v1_report(repo_root, output_dir)

    bundle = {
        "repository": args.repository,
        "pull_request": {
            "number": args.pr_number,
            "title": args.pr_title,
            "author": args.pr_author,
            "base_ref": args.base_ref,
            "head_ref": args.head_ref,
        },
        "changed_files": changed_files,
        "diff": diff_text[: config.get("inputs", {}).get("max_diff_chars", 150000)],
        "policy_docs": policy_docs,
        "prompts": prompts,
        "config": config,
        "supporting_v1": v1_context,
        "instruction": (
            "Bob must produce the authoritative review result. "
            "No local script should determine block_merge."
        ),
    }

    bundle_file = output_dir / "bob-pr-review-bundle.json"
    write_json(bundle_file, bundle)

    print(json.dumps({
        "bundle_file": str(bundle_file),
        "changed_file_count": len(changed_files),
        "v1_context_used": v1_context.get("executed", False),
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

# Made with Bob
