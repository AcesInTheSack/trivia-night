import json, glob, os, sys, collections
sys.path.insert(0, os.path.dirname(__file__))
from import_questions import normalize
rows=[]
bank=json.load(open(os.path.join(os.path.dirname(__file__), "..", "TriviaNight", "Resources", "questions.json")))
for c in bank["categories"]:
    for q in c["questions"]: rows.append(("bank",c["name"],q["d"],q["q"],q["a"]))
for f in sorted(glob.glob(os.path.join(os.path.dirname(__file__), "..", "questions", "raw", "**", "*.txt"), recursive=True)):
    cat=os.path.basename(f)[:-4]
    for line in open(f,encoding="utf-8"):
        if line.strip():
            d,q,a=line.strip().split("|"); rows.append(("new",cat,int(d),q,a))
qn=[" "+normalize(r[3])+" " for r in rows]
# same-answer pairs within the same category (likely asking about the same thing)
byans=collections.defaultdict(list)
for i,r in enumerate(rows):
    a=normalize(r[4]); 
    if len(a)>2: byans[(r[1],a)].append(i)
print("SAME ANSWER, SAME CATEGORY:")
for (cat,a),ix in sorted(byans.items()):
    if len(ix)>1 and any(rows[i][0]=="new" for i in ix):
        print(f"  [{cat}] {a}: " + " || ".join(f"({rows[i][0]}) {rows[i][3]}" for i in ix))
print("\nANSWER GIVEN AWAY IN ANOTHER QUESTION:")
for i,r in enumerate(rows):
    a=normalize(r[4])
    a=a[4:] if a.startswith("the ") else a
    if len(a)<5: continue
    for j,q in enumerate(qn):
        if i!=j and not (r[0]=="bank" and rows[j][0]=="bank") and f" {a} " in q:
            print(f"  ({r[0]}) {r[3]} -> {r[4]}\n      leaked by ({rows[j][0]}/{rows[j][1]}) {rows[j][3]}")
