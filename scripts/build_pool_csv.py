#!/usr/bin/env python3
"""Combine questions/raw/*.txt (lines of "difficulty|question|answer") into one review CSV.

Usage: python3 scripts/build_pool_csv.py OUT.csv
Always includes questions/original-300.csv (the first questions, IDs kept as-is).
Then load it into the app with: python3 scripts/import_questions.py OUT.csv
"""
import csv, glob, json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from import_questions import question_id, ICONS

out = sys.argv[1]
rows = []
with open(os.path.join(HERE, "..", "questions", "original-300.csv"), encoding="utf-8-sig") as f:
    for r in csv.DictReader(f):
        rows.append((r["id"], r["category"], int(r["difficulty"]), r["question"], r["answer"]))
order = list(ICONS)
for path in sorted(glob.glob(os.path.join(HERE, "..", "questions", "raw", "**", "*.txt"), recursive=True)):
    cat = os.path.splitext(os.path.basename(path))[0]
    for n, line in enumerate(open(path, encoding="utf-8"), 1):
        if not line.strip():
            continue
        d, q, a = line.rstrip("\n").split("|")
        rows.append((question_id(q), cat, int(d), q.strip(), a.strip()))
rows.sort(key=lambda r: (order.index(r[1]) if r[1] in order else 99, r[1], r[2]))
with open(out, "w", newline="", encoding="utf-8-sig") as f:
    w = csv.writer(f)
    w.writerow(["id", "category", "difficulty", "question", "answer"])
    w.writerows(rows)
print(f"{len(rows)} rows -> {out}")
