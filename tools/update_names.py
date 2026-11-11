#!/usr/bin/env python3
"""Regenerate app/src/main/assets/names.json (game data ID -> display name).

Sources (both MIT-licensed, community-maintained):
  1. chiefpansancolt/clash-of-clans-data  - updated often; preferred.
  2. mathsman5133/coc.py static_data.json - fills any gaps.

Usage:  python3 tools/update_names.py
Re-run after a game update adds new buildings, troops or equipment.
Standard library only.
"""
import json
import pathlib
import sys
import urllib.request
from concurrent.futures import ThreadPoolExecutor

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "app/src/main/assets/names.json"

CCD_REPO = "chiefpansancolt/clash-of-clans-data"
CCD_TREE = f"https://api.github.com/repos/{CCD_REPO}/git/trees/main?recursive=1"
CCD_RAW = f"https://raw.githubusercontent.com/{CCD_REPO}/main/"
COCPY_STATIC = "https://raw.githubusercontent.com/mathsman5133/coc.py/master/coc/static/static_data.json"
COCPY_CATEGORIES = ["buildings", "traps", "troops", "guardians", "spells", "heroes", "pets", "equipment", "helpers"]


def fetch_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": "coc-trackerwidget-names-updater"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def collect_data_ids(node, out):
    """Find every {"dataId": int, "name": str} object anywhere in a JSON tree."""
    if isinstance(node, dict):
        if isinstance(node.get("dataId"), int) and isinstance(node.get("name"), str):
            out.setdefault(node["dataId"], node["name"])
        for v in node.values():
            collect_data_ids(v, out)
    elif isinstance(node, list):
        for v in node:
            collect_data_ids(v, out)


def from_ccd():
    tree = fetch_json(CCD_TREE)["tree"]
    paths = [
        t["path"] for t in tree
        if t["path"].startswith(("data/home/", "data/builder/"))
        and t["path"].endswith(".json")
        and "/leagues/" not in t["path"]
    ]
    names = {}
    with ThreadPoolExecutor(max_workers=16) as pool:
        for doc in pool.map(lambda p: fetch_json(CCD_RAW + p), paths):
            collect_data_ids(doc, names)
    return names


def from_cocpy():
    data = fetch_json(COCPY_STATIC)
    return {x["_id"]: x["name"] for cat in COCPY_CATEGORIES for x in data.get(cat, []) if "_id" in x}


def main():
    ccd = from_ccd()
    cocpy = from_cocpy()
    print(f"clash-of-clans-data: {len(ccd)} IDs, coc.py: {len(cocpy)} IDs")

    conflicts = {i: (ccd[i], cocpy[i]) for i in ccd.keys() & cocpy.keys() if ccd[i] != cocpy[i]}
    for i, (a, b) in sorted(conflicts.items()):
        print(f"  name differs for {i}: '{a}' (used) vs coc.py '{b}'")

    merged = {**cocpy, **ccd}  # fresher source wins
    OUT.write_text(json.dumps({str(k): merged[k] for k in sorted(merged)}, indent=1, ensure_ascii=False) + "\n")
    print(f"Wrote {len(merged)} names to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    sys.exit(main())
