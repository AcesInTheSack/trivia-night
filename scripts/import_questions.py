#!/usr/bin/env python3
"""Build TriviaNight/Resources/questions.json from a question pool, giving every question a
permanent ID and dropping duplicates.

Input: one or more CSV or JSON files.
  CSV  needs columns category, difficulty, question, answer (header names are
       case-insensitive; d/q/a also work). An optional "id" column is kept as-is.
  JSON may be a list of {category, difficulty, question, answer[, id]} objects, or the
       app's own questions.json format.

IDs: "q" + the first 12 hex characters of a SHA-1 of the normalized question text.
The same question always gets the same ID, so re-importing a pool, or adding to it later,
never changes the IDs of questions the app has already shown. Two questions that differ
only in capitalization, punctuation or spacing count as duplicates.

Usage:
  python3 scripts/import_questions.py pool.csv [more.csv ...] [--merge] [--out PATH]

  --merge   keep the questions already in the output file and add the new ones
"""
import argparse
import csv
import hashlib
import json
import re
import sys
import unicodedata
from collections import OrderedDict
from pathlib import Path

DEFAULT_OUT = Path(__file__).resolve().parent.parent / "TriviaNight" / "Resources" / "questions.json"

ICONS = {
    "History": "🏛️", "Science": "🔬", "Television": "📺", "Pop Culture": "✨",
    "Movies & Entertainment": "🎬", "Music": "🎵", "Geography": "🌍", "Sports": "🏆",
    "Literature": "📚", "Art & Architecture": "🎨", "Food & Drink": "🍷",
    "Animals & Nature": "🦉", "Technology": "💻", "Mythology": "⚡", "Words & Language": "🔤",
}


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c)).lower()
    text = re.sub(r"[.'’`]", "", text)          # U.S. -> us, don't -> dont
    text = re.sub(r"[^a-z0-9 ]+", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def question_id(text: str) -> str:
    return "q" + hashlib.sha1(normalize(text).encode()).hexdigest()[:12]


def pick(row: dict, *names):
    lowered = {k.strip().lower(): v for k, v in row.items() if k}
    for n in names:
        if n in lowered and lowered[n] not in (None, ""):
            return str(lowered[n]).strip()
    return ""


def read_rows(path: Path):
    if path.suffix.lower() == ".json":
        data = json.loads(path.read_text(encoding="utf-8"))
        if isinstance(data, dict) and "categories" in data:
            for cat in data["categories"]:
                for q in cat["questions"]:
                    yield {"category": cat["name"], "difficulty": q["d"], "question": q["q"],
                           "answer": q["a"], "id": q.get("id", "")}
        else:
            yield from data
    else:
        with path.open(newline="", encoding="utf-8-sig") as f:
            yield from csv.DictReader(f)


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("inputs", nargs="+", type=Path)
    ap.add_argument("--merge", action="store_true")
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    args = ap.parse_args()

    categories: "OrderedDict[str, list]" = OrderedDict()
    icons: dict = dict(ICONS)
    seen_ids: dict = {}        # id -> (category, question text)
    problems, dupes, conflicts = [], 0, []

    sources = ([args.out] if args.merge and args.out.exists() else []) + args.inputs
    if args.merge and args.out.exists():
        for cat in json.loads(args.out.read_text(encoding="utf-8"))["categories"]:
            icons.setdefault(cat["name"], cat.get("icon", "❓"))

    for src in sources:
        for n, row in enumerate(read_rows(src), start=2):
            cat = pick(row, "category", "cat")
            q = pick(row, "question", "q")
            a = pick(row, "answer", "a")
            d = pick(row, "difficulty", "d", "level")
            where = f"{src.name}:{n}"
            if not (cat and q and a and d):
                problems.append(f"{where}: missing category, question, answer or difficulty")
                continue
            try:
                d = int(float(d))
            except ValueError:
                problems.append(f"{where}: difficulty '{d}' isn't a number")
                continue
            if not 1 <= d <= 10:
                problems.append(f"{where}: difficulty {d} is outside 1-10")
                continue
            qid = pick(row, "id") or question_id(q)
            if qid in seen_ids:
                dupes += 1
                prev_cat, prev_q, prev_a = seen_ids[qid]
                if normalize(prev_a) != normalize(a):
                    conflicts.append(f"{where}: duplicate of “{prev_q}” with a different answer (“{prev_a}” vs “{a}”); kept the first")
                continue
            seen_ids[qid] = (cat, q, a)
            categories.setdefault(cat, []).append({"id": qid, "d": d, "q": q, "a": a})

    out = {"version": 2, "categories": [
        {"name": name, "icon": icons.get(name, "❓"), "questions": qs}
        for name, qs in categories.items()
    ]}
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(out, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")

    total = sum(len(qs) for qs in categories.values())
    print(f"Wrote {total} questions in {len(categories)} categories to {args.out}")
    print(f"Duplicates skipped: {dupes}")
    for name, qs in categories.items():
        levels = sorted({q['d'] for q in qs})
        print(f"  {name}: {len(qs)} (difficulty {levels[0]}-{levels[-1]})")
    for label, items in (("Rows skipped", problems), ("Duplicates with different answers", conflicts)):
        if items:
            print(f"\n{label} ({len(items)}):")
            for line in items[:50]:
                print("  " + line)
            if len(items) > 50:
                print(f"  ... and {len(items) - 50} more")
    return 0


if __name__ == "__main__":
    sys.exit(main())
