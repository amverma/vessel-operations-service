#!/usr/bin/env python3
"""
Validate Bob review artifacts for the V3 fully agentic flow.

This script does not analyze code.
It only verifies that Bob produced the required decision output contract.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path


REQUIRED_TOP_LEVEL_KEYS = {
    "status",
    "confidence",
    "summary",
    "findings",
    "positives",
    "block_merge",
}


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Bob review result")
    parser.add_argument("--json-result", required=True, help="Path to Bob JSON result")
    parser.add_argument("--markdown-result", required=True, help="Path to Bob Markdown review")
    args = parser.parse_args()

    json_path = Path(args.json_result)
    markdown_path = Path(args.markdown_result)

    if not json_path.exists():
        print("Missing Bob JSON result")
        return 2

    if not markdown_path.exists():
        print("Missing Bob Markdown review")
        return 2

    data = json.loads(json_path.read_text(encoding="utf-8"))
    missing = sorted(REQUIRED_TOP_LEVEL_KEYS - set(data.keys()))
    if missing:
        print(f"Bob JSON result is missing required keys: {', '.join(missing)}")
        return 2

    if not isinstance(data["findings"], list):
        print("Bob JSON result field 'findings' must be a list")
        return 2

    if not isinstance(data["positives"], list):
        print("Bob JSON result field 'positives' must be a list")
        return 2

    if not isinstance(data["block_merge"], bool):
        print("Bob JSON result field 'block_merge' must be boolean")
        return 2

    status = str(data["status"]).upper()
    if status not in {"PASS", "FAIL", "NEEDS_ATTENTION"}:
        print("Bob JSON result field 'status' must be PASS, FAIL, or NEEDS_ATTENTION")
        return 2

    print("Bob review artifacts are structurally valid")

    if data["block_merge"]:
        print("Bob decided to block merge")
        return 1

    print("Bob approved or did not block merge")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

# Made with Bob
