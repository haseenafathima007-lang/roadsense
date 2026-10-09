# Phase 1: Data hardening
**Branch:** `phase/01-data-hardening` | **Builder:** Gemini 3.1 Pro | **Verifier:** Claude Sonnet 5.5 | **Quota:** medium | **Depends on:** Phase 0

## Goal
A trustworthy, leakage-aware, audited dataset in YOLO format from RDD2022, with every labelling caveat written down.

## You do first (manual)
Download RDD2022 from https://doi.org/10.6084/m9.figshare.21431547 into `data/raw/RDD2022/` (or let the agent do it after you approve; the files are large).
Check the licence on the figshare page and note it in `docs/DATASETS.md`. Optionally clone https://github.com/sekilab/RoadDamageDetector (outside this repo) for reference.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (foundation fixes in section 3), docs/DATASETS.md. Image-only project. PHASE 1: data hardening.
RDD2022 is in data/raw/RDD2022/ (or ask me before downloading anything over 500 MB). Do not commit data.

STEP 1: Implementation Plan, wait for approval.
STEP 2: write Python scripts under ai-service/training/data/ (plus tests under ai-service/tests/ using tiny synthetic fixtures):
1. inventory.py: walk the dataset, parse PASCAL VOC XML, output docs/data_audit.json and docs/data_audit.md with: images per country and split
   folder, annotations per class per country, class names found that are NOT in {D00,D10,D20,D40} (excluded-class label audit: counts, example
   file names, and an explicit include/exclude decision per class that I approve), images with no annotations, corrupt images, size distribution.
2. Report which public splits have no labels (test sets) and state that we build our own labelled split.
3. grouped_split.py: create train/val/test from labelled data with a GROUPED split so near-identical consecutive photos never straddle splits.
   There is no sequence ID, so propose a grouping heuristic (e.g. filename index blocks per country + perceptual-hash near-duplicate clustering),
   explain its limits, and MEASURE leakage: after splitting, compute the share of test images whose nearest perceptual-hash neighbour is in train
   at a stated distance threshold; write it into docs/data_audit.json. Fixed seed. Output split lists to data/processed/splits/*.txt (not committed) and a hash of the lists.
4. convert_to_yolo.py: two schemes selectable by flag. rdd4 = D00,D10,D20,D40 as-is. rdd3 = merge D00+D10 into crack_linear, D20 alligator, D40 pothole
   (this implements the "D10 mitigation": orientation is decided later in Java). Write data/processed/yolo_{rdd3,rdd4}/ with images (symlinks ok), labels, data.yaml.
   Clip boxes, drop degenerate boxes, and log every dropped annotation.
5. Document in docs/DATASETS.md: D40 is broader than "pothole" (rutting, bump, separation, pothole); the mapping is an approximation and a stated limitation.
6. Placeholder only: docs/PATCH_CLASS_PLAN.md describing how a "patch" hard-negative class will be added from our own photos in Phase 2/3 (do not invent data).
7. A union-area damage index is Java work for Phase 5; only record it in docs/ROADMAP notes if missing.
RULES: no hand-typed statistics anywhere; all numbers come from docs/data_audit.json. Pin and record any new Python dependency in docs/DEPENDENCIES.md.
OUTPUT: files created, commands run with real output, the excluded-class table, the leakage number, and open questions for me.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 1. Follow the common verifier rules in docs/prompts/00_START_HERE.md. Write docs/verification/phase-01.md.
Checks:
1. `cd ai-service && ruff check . && pytest -q` pass.
2. Re-run inventory.py and confirm docs/data_audit.json reproduces (same counts). Quote totals per country and per class.
3. Spot-check 20 random XML files by hand against the audit counts (list the files and findings).
4. Excluded-class audit exists, lists every non-{D00,D10,D20,D40} class found (or states none found with evidence), with decisions.
5. Splits: no image id appears in two splits (script proof). Leakage measurement exists, method is stated, number comes from the JSON.
6. YOLO conversion: for both schemes, count label files == count images used; every class id in labels < nc; boxes normalised in [0,1]; dropped annotations logged. Show data.yaml for both.
7. rdd3 mapping really merges D00+D10 and nothing else (verify on 10 sampled files).
8. docs/DATASETS.md states D40 != pothole-only and lists the licence found.
9. No dataset files, weights or images are tracked by git (`git ls-files | grep -Ei "\.(jpg|png|pt|onnx)$"` must be empty or only approved fixtures).
10. Every number in docs/data_audit.md matches docs/data_audit.json.
Verdict + defects.
````

## EXPECTED OUTPUTS
- `docs/data_audit.json` / `.md`, scripts `inventory.py`, `grouped_split.py`, `convert_to_yolo.py`, tests, `docs/PATCH_CLASS_PLAN.md`.
- Locally: `data/processed/yolo_rdd3/data.yaml` (nc=3), `data/processed/yolo_rdd4/data.yaml` (nc=4), split lists.
- Leakage figure in JSON (expect a non-zero value you must interpret; do not tune it to zero by hiding data).
- Pass criteria: reproducible audit, zero cross-split id overlap, every conversion drop logged.

## Commit / PR
`feat(data): audit, grouped split and yolo conversion for RDD2022`
