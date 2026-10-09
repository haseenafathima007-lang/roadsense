# Phase 3: Evaluation and Chennai out-of-distribution set
**Branch:** `phase/03-evaluation-chennai` | **Builder:** Gemini 3.1 Pro | **Verifier:** Claude Sonnet 5.5 | **Quota:** medium | **Depends on:** Phase 2 (weights) and your photos

## Goal
Honest numbers on the grouped test split and on your own Chennai photos, produced only by scripts. Photo collection protocol with EXIF kept.

## You do (manual)
Collect 15-30+ road photos in Chennai following the protocol the agent writes (EXIF location on; do not send via messengers that strip EXIF; include fresh patches,
shadows, wet roads, clean road negatives). Label them (CVAT/Label Studio). Keep them private under `data/own/chennai/`.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (sections 3, 13), docs/RESULTS.md. PHASE 3: evaluation + Chennai out-of-distribution set. Image-only.
STEP 1: Implementation Plan, wait for approval.
STEP 2:
1. ai-service/eval/evaluate.py: on the grouped TEST split only, compute per-class precision, recall, mAP@0.5, mAP@0.5:0.95, overall and per country; confusion matrix;
   write docs/test_metrics.json including model file hash, dataset split hash, config, git commit, date, conf/IoU thresholds used. Save a failure gallery (worst FP/FN) under runs/ (not committed).
2. Same script with --dataset chennai writes docs/chennai_ood_metrics.json.
3. scripts/exif_audit.py: over data/own/chennai/, count photos with usable GPS EXIF, missing, zero/implausible coordinates; write docs/exif_audit.json. (This feeds the "share with usable EXIF GPS" metric.)
4. docs/CHENNAI_COLLECTION_PROTOCOL.md: capture protocol (consistent distance/height/pitch assumptions marked as hypotheses), how to keep EXIF, privacy (avoid faces/plates; blurring before sharing), recording sheet (photo id, date, road, condition, conditions), minimum counts per class and negatives, two-person labelling for later agreement.
5. scripts/make_results.py: regenerates docs/RESULTS.md ONLY from docs/*.json; any metric without a source file renders "NOT YET MEASURED". Unit-test it (missing file -> NOT YET MEASURED; present file -> value + source).
6. Report calibration/threshold choices as assumptions; do not tune the conf threshold on the test split (use validation split; write what you did).
RULES: no test-split tuning, no hand-typed numbers, document any limitation (D40 != pothole-only, spot bias).
OUTPUT: files, commands + real output, the metrics JSON paths, caveats.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 3. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-03.md.
Checks:
1. ruff + pytest pass.
2. Re-run evaluate.py on the test split; compare to docs/test_metrics.json (allow tiny numeric noise; state tolerance). Quote per-class numbers.
3. Prove the test split was not used for threshold/hyper-parameter selection (inspect scripts, configs, logs).
4. JSON contains provenance fields (model hash, split hash, commit, date, thresholds).
5. Chennai metrics file and EXIF audit exist only if photos exist; otherwise RESULTS.md must say NOT YET MEASURED for them. Check which case applies.
6. scripts/make_results.py: delete a JSON temporarily (restore afterwards) and confirm the row falls back to NOT YET MEASURED; run its tests.
7. Every number in docs/RESULTS.md matches a JSON field (list mismatches).
8. Collection protocol covers EXIF retention, privacy, negatives, labelling plan.
9. No photos or labels from data/own/ tracked by git.
Verdict + defects.
````

## EXPECTED OUTPUTS
- `docs/test_metrics.json`, (after photos) `docs/chennai_ood_metrics.json`, `docs/exif_audit.json`, regenerated `docs/RESULTS.md`, protocol doc, tests.
- Pass criteria: numbers reproducible within stated tolerance, provenance present, test split untouched for tuning, unmeasured rows still say NOT YET MEASURED.

## Commit / PR
`feat(eval): test and Chennai OOD evaluation with provenance`
