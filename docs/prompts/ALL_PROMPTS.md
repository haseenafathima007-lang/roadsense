# ALL PROMPTS (convenience copy; the per-phase files in docs/prompts/ are the source)

# Phase 0: Repo standards
**Branch:** `phase/00-repo-standards` | **Builder:** Gemini 3.8 Flash | **Verifier:** Gemini 3.1 Pro | **Quota:** light | **Depends on:** scaffold committed

## Goal
Working Java + Python skeletons with green CI, so every later phase starts from a clean, checked base.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md, docs/ARCHITECTURE.md, docs/ROADMAP.md. This project is image-only,
Java-centred (Python = stateless YOLO endpoint, Java = decision engine). You are doing PHASE 0 only: repo standards. Implement NO features.

STEP 1 (plan first): produce an Implementation Plan listing every file you will create/change. Wait for my approval.

STEP 2 (after approval):
a) java-app/: Maven project, groupId com.roadai, artifactId road-ai-app, Java 21. Create one package per entry in design section 4.1
   (domain, perception, imaging, geo, analysis, priority, lifecycle, persistence, service, security, config, ui) with a package-info.java
   that states the package's responsibility in one sentence. Add JUnit 5, AssertJ, JaCoCo (report on verify), Spotless with
   google-java-format (check on verify). Add ONE smoke test. Look up the current stable plugin/dependency versions yourself
   (do not rely on memory) and record name, version, licence, date checked in docs/DEPENDENCIES.md.
b) ai-service/: pyproject.toml (project metadata, optional-dependencies [dev] with ruff and pytest), app/__init__.py,
   tests/test_smoke.py, ruff config. No model or FastAPI code yet.
c) Make .github/workflows/ci.yml work: run exactly its commands locally and show the output.
d) docs/uml/README.md explaining the diagram convention (Mermaid or PlantUML; pick one, record it as docs/adr/0003-uml-tool.md).
e) Update docs/PHASE_STATUS.md only after I confirm.
f) Do not delete docs/03_PLACE_DESIGN_DOC_HERE.md unless docs/03_image_only_java_redesign.md exists; if it is missing, tell me.

RULES: no network downloads other than Maven/pip dependencies; no secrets; conventional commits on this branch; never touch main.

OUTPUT: (1) list of files created, (2) every command you ran with its real output, (3) anything you could not do and why.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 0 of this repo. Follow the common verifier rules in docs/prompts/00_START_HERE.md
(read-only on source; write only docs/verification/phase-00.md; PASS/FAIL/NOT RUN with quoted evidence).
Checks:
1. docs/03_image_only_java_redesign.md exists. 
2. `cd java-app && mvn -B verify` succeeds. Quote the final BUILD line and test count.
3. java-app has all 12 packages from design section 4.1, each with package-info.java.
4. JaCoCo report generated (path) and Spotless check runs as part of verify.
5. `cd ai-service && pip install -e ".[dev]" && ruff check . && pytest -q` succeed.
6. .github/workflows/ci.yml commands match what you just ran; YAML parses.
7. docs/DEPENDENCIES.md rows exist for every dependency added, with licence and date filled. List any missing.
8. No video/tracker/GPX wording introduced anywhere (grep -ri "bytetrack|gpx|video" excluding docs/03_* and ADR 0001).
9. .gitignore prevents committing data/, *.pt, *.onnx, runs/ (prove with `git check-ignore -v`).
10. No secrets or large files committed (`git ls-files | xargs du -ch | tail -1`, and a grep for key-like strings).
Finish with a one-paragraph verdict and the list of defects.
````

## EXPECTED OUTPUTS
- `java-app/pom.xml`, 12 packages with `package-info.java`, `SmokeTest`, `target/site/jacoco/` after verify.
- `ai-service/pyproject.toml`, `tests/test_smoke.py`.
- `docs/DEPENDENCIES.md` rows filled; `docs/adr/0003-uml-tool.md`.
- Commands: `mvn -B verify` -> `BUILD SUCCESS`, 1 test; `pytest -q` -> `1 passed`; `ruff check .` -> `All checks passed!`.
- `docs/verification/phase-00.md` with 10 PASS rows.

## Commit / PR
`chore: add java and python skeletons, CI and dependency register` -> PR -> merge -> tick Phase 0.

---

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

---

# Phase 2: Training (local, inside Antigravity)
**Branch:** `phase/02-training` | **Builder:** Gemini 3.1 Pro | **Verifier:** Claude Sonnet 5.5 | **Quota:** medium | **Depends on:** Phase 1

## Goal
Reproducible, resumable training that runs on YOUR computer through the Antigravity terminal, sized to your hardware and time budget. Colab/Kaggle is an optional fallback only.

## You do (manual)
- Tell the agent your OS and how long you can leave the machine training (e.g. "overnight, 8 hours"). Keep the laptop plugged in and stop it from sleeping.
- Decide the licence implications of Ultralytics (AGPL-3.0, verify) before choosing it; the agent will flag alternatives.

## Honest expectations
Training uses your machine, not Google's. An NVIDIA GPU or Apple Silicon (MPS) makes a small model feasible; CPU-only will be very slow, so the prompt makes the agent measure first and shrink the plan (smaller model, smaller image size, data subset, fewer epochs) rather than guess.
Long runs must be launched in the background so the agent does not block or burn quota watching them.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md, docs/DATASETS.md, docs/data_audit.md. PHASE 2: training, run locally on this machine through your terminal. Image-only, YOLO detector.
STEP 0: ask me (1) my OS, (2) my time budget for the main training run, (3) whether I accept the licence of the training library after you present it. Do not assume.
STEP 1: Implementation Plan (model family options with licence notes and one recommendation). Wait for approval.
STEP 2: build and run:
1. ai-service/training/hw_probe.py: detect CUDA GPU / Apple MPS / CPU, VRAM or RAM, free disk, library versions; print a plain-language verdict and the device string to use. Run it and show me the output.
2. ai-service/training/train.py: config-driven (YAML in ai-service/training/configs/), fixed seed, schemes rdd3/rdd4, device from hw_probe or a flag, checkpoint every epoch, resumable (--resume continues from last checkpoint).
   Logs hardware, library versions, git commit, dataset split hash, command line, start/end time into runs/<name>/run_info.json. No hard-coded paths. Smoke mode: 1 epoch on 50 images.
3. ai-service/training/benchmark.py: time a short run (e.g. a few hundred training images, 1 epoch) on this machine and extrapolate the time per full epoch. Show me the measured numbers and the extrapolation method; label it an estimate.
4. PROPOSE a training plan that fits my time budget from the benchmark (model size, image size, data fraction, epochs, batch size) with the trade-offs, and wait for my approval. Do not start the long run before I approve.
5. scripts/run_training_background.* for my OS: launch the approved run DETACHED (nohup or tmux on macOS/Linux, with caffeinate on macOS to prevent sleep; Start-Process on Windows), writing runs/<name>/train.log and a PID file.
   Never block the terminal on the run. After launching, report how to check progress and stop. Check the log at most every 15-20 minutes while I am talking to you; otherwise tell me to come back.
6. Interruption test: in smoke mode, kill the run mid-way and prove --resume continues from the checkpoint.
7. export.py: export best weights to ONNX plus a tiny parity-check script (Python vs ONNX on N images); needed for the Phase 10 gate, do not run the gate now.
8. docs/TRAINING_LOG.md: table (date, config, seed, dataset hash, git hash, hardware, epochs, notes) filled ONLY from real run_info.json files.
9. Patch hard-negative class: docs/PATCH_CLASS_PLAN.md + a labelling guide (CVAT or Label Studio; verify the tool) and a merge script adding a `patch` class from my own labelled photos to the rdd3 scheme, tested on synthetic fixtures. Do not fabricate photos.
10. OPTIONAL appendix docs/COLAB_FALLBACK.md: step-by-step Colab/Kaggle instructions for a beginner, only if local training turns out infeasible. Do not make it a dependency of anything.
RULES: weights/runs never committed. No performance numbers in docs unless copied from a run file by script. Report training results only after the run finishes.
OUTPUT: hw_probe output, benchmark numbers, the proposed plan awaiting my approval, files created, commands run with real output, open questions.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 2. Follow docs/prompts/00_START_HERE.md rules. Write docs/verification/phase-02.md.
Checks:
1. ruff + pytest pass in ai-service.
2. Run hw_probe.py; the device it picks is consistent with the machine (state what it detected).
3. Run the CPU/smoke training: it completes and writes runs/<name>/run_info.json with all required fields (list them).
4. Resume test: start a smoke run, interrupt, resume; show the epoch continues rather than restarting.
5. Same seed twice in smoke mode gives the same dataset split hash and config hash (state what is and is not deterministic).
6. benchmark.py output exists, labels the extrapolation as an estimate, and the proposed plan cites its numbers.
7. Background launcher: starts detached, writes train.log and a PID file, and the agent's terminal is not left blocked (inspect the script; run a 1-minute smoke launch and stop it).
8. No absolute paths or user names hard-coded (grep).
9. export.py and the parity script run on smoke weights (if exported); report the output as printed.
10. docs/TRAINING_LOG.md has no number without a matching run_info.json.
11. Patch-class merge test passes on synthetic fixtures and never modifies rdd3 originals (hash proof).
12. Licence note for the training library is in docs/DEPENDENCIES.md.
13. .gitignore keeps weights/runs out of git (git status after the smoke run shows no .pt or runs/).
Verdict + defects.
````

## EXPECTED OUTPUTS
- `hw_probe.py`, `train.py`, `benchmark.py`, `export.py`, background launcher, configs, tests, `docs/TRAINING_LOG.md`, `docs/COLAB_FALLBACK.md` (optional).
- On your machine after approval: `runs/<name>/best.pt`, `run_info.json`, `train.log`, results CSV (never committed).
- Pass criteria: smoke run and resume work; the long run is detached; the plan is justified by a measured benchmark; no unmeasured claims.

## Commit / PR
`feat(training): local resumable training, benchmark and patch-class tooling`

---

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

---

# Phase 4: Perception service + input pipeline
**Branch:** `phase/04-perception-inputs` | **Builder:** Claude Sonnet 5.5 | **Verifier:** Gemini 3.1 Pro | **Quota:** medium | **Depends on:** Phases 0, 2 (weights optional: tests use fakes)

## Goal
Python exposes one hardened endpoint. Java gets the domain value types, detector clients, EXIF reading, quality gates and the location chain, all testable without Python or network.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (sections 4, 5.1, 5.5, 5.2 first stages), docs/CONTRACTS.md. PHASE 4: perception + inputs. Image-only.
STEP 1: Implementation Plan (include chosen libraries with licence + date in docs/DEPENDENCIES.md). Wait for approval.
STEP 2, Python (ai-service/app/): FastAPI with GET /health, POST /v1/detect/image, optional POST /v1/detect/batch exactly as docs/CONTRACTS.md.
  API key header, upload size/type limits, error shape from CONTRACTS.md. Detector behind an injectable interface so tests use a stub (no weights needed).
  Weights path via env. Tests: auth fail, wrong type, oversize, happy path with stub, error shape. No business logic.
STEP 3, Java (com.roadai.*), all with JUnit 5 + AssertJ:
  domain: records GeoPoint, BoundingBox, LocationFix(source, accuracyM nullable), CaptureMeta, enum DamageClass, Report, Observation (class, confidence, box, frame size).
  perception: DamageDetector interface, RemoteYoloDetector (java.net.http, timeouts, maps the error shape to ApiException), FakeDetector (scripted outputs), DetectorFactory.
  imaging: ExifReader interface + MetadataExtractorReader (candidate library: verify), ExifResult (time, GPS, camera) tolerant of missing/garbage data;
    QualityGate interface + BlurGate (Laplacian variance), ExposureGate (mean luma), SizeGate, FramingGate (truncated boxes touching image edges within a margin);
    gates FLAG with reasons, never silently drop. All thresholds from a Thresholds object loaded once from thresholds.yaml (immutable). Missing required keys: fail fast with a clear message.
  geo: LocationResolver as a Chain of Responsibility: ExifLocation -> DeviceLocation -> ManualPin; store location_source + accuracy; missing location is allowed (state = LOCATION_NEEDED).
  Test fixtures: generate small JPEGs programmatically (sharp, blurred, dark, tiny, with/without GPS EXIF). Document how EXIF fixtures are produced. No real photos.
  A RemoteYoloDetector test must use a local stub HTTP server (JDK HttpServer or similar), not the real service.
RULES: ui untouched; no persistence yet; no dedup yet; no video wording; mvn verify and pytest must be green before each commit.
OUTPUT: files, commands + real output, assumptions that need my decision.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 4. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-04.md.
Checks:
1. `cd ai-service && ruff check . && pytest -q` pass; list tests for auth, type, size, error shape, happy path.
2. Start the service with the stub or real weights; curl /health and POST /v1/detect/image with a valid and an invalid file; quote responses; they match docs/CONTRACTS.md.
3. `cd java-app && mvn -B verify` passes; quote test count and JaCoCo line coverage for imaging, geo, perception packages (as measured, no rounding up).
4. Interfaces used for injection: show that no class constructs RemoteYoloDetector or MetadataExtractorReader directly outside DetectorFactory/composition code.
5. LocationResolver chain: demonstrate via tests EXIF-present, EXIF-missing+device, none -> LOCATION_NEEDED, manual pin with source recorded.
6. Quality gates flag with reasons and never remove the report; show test evidence for blur, dark, tiny, truncated cases.
7. All thresholds come from Thresholds, no magic numbers in gate classes (grep).
8. Exif reader tolerates corrupt/empty EXIF without throwing uncaught exceptions (test evidence).
9. domain package has no java.io / java.net / java.sql imports (grep).
10. Java tests need no network and no Python process (run with network disabled if possible, else inspect).
11. New dependencies recorded in docs/DEPENDENCIES.md with licence.
Verdict + defects.
````

## EXPECTED OUTPUTS
- Python: `app/main.py`, `schemas.py`, `detector.py`, tests. Java: classes listed above + fixtures generator + tests.
- Commands: `pytest -q` and `mvn -B verify` green; curl `/health` returns `{status, model, scheme}`.
- Pass criteria: contract matched exactly; every gate has positive and negative tests; location chain covers all four outcomes.

## Commit / PR
`feat: hardened detection endpoint, exif, quality gates and location chain`

---

# Phase 5: Analysis (observations -> defects, dedup, best view, severity)
**Branch:** `phase/05-analysis` | **Builder:** plan Claude Opus 5.5, build Claude Sonnet 5.5 | **Verifier:** Gemini 3.1 Pro | **Quota:** heavy (plan), medium (build) | **Depends on:** Phase 4

## Goal
The core of the project: counting defects instead of photos, with a safe "needs review" default.

## You provide
Severity area-ratio thresholds and escalation rules from your main document section 5, so the agent can fill `severity.*` in `thresholds.yaml`. If you do not, the agent must stop and ask.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (sections 4.1, 4.2, 5.3, 5.4, 5.8 for survey notes, 3 for union area), thresholds.yaml. PHASE 5: analysis. Java only.
STEP 0: severity thresholds in thresholds.yaml are null. STOP and ask me for the values from the main document section 5 before implementing SeverityStrategy.
STEP 1 (use your strongest reasoning): produce an Implementation Plan with the class diagram, the dedup decision table (all branches), and test list. Wait for approval.
STEP 2: implement in com.roadai.domain/analysis/imaging:
 - Defect (abstract) with subclasses Pothole, LongitudinalCrack, TransverseCrack, AlligatorCrack (escalation(), weight()). Guarded state; observations added only via methods.
 - CrackOrientationClassifier: decides D00 vs D10 family orientation from the crack box/shape (document the heuristic as a hypothesis; the detector scheme may be rdd3).
 - ImageSimilarity interface + OrbSimilarity (candidate OpenCV; verify licence and platform support) and a PerceptualHashSimilarity fallback behind the same interface. Scores 0..1. Unvalidated.
 - DefectAssembler implementing design 5.3 exactly: same crack family; distance <= match_radius widened by accuracy_factor x accuracy, capped at max_radius; manual pins use the separate larger radius;
   similarity >= min_similarity when both photos exist and passed quality; similarity unavailable or ambiguous -> do NOT merge, create NEEDS_REVIEW link. Default for doubt is review.
 - Haversine and SpatialIndex (grid or similar) so matching is not O(n^2) for large sets; test correctness against brute force.
 - BestViewSelector (design 5.4): prefer non-truncated, quality-passing, largest area, highest in near zone; flag when only poor views exist.
 - SeverityStrategy interface, AreaRatioSeverity and BestViewSeverity: Low/Medium/High from area ratio + escalation, thresholds from Thresholds only. No physical size/depth claims in names or docs.
 - UnionAreaCalculator: area of the union of overlapping boxes (coordinate compression or sweep). Property tests: union <= sum, union >= max, order-independent, identical boxes -> one box area.
 - LinkSummaryBuilder skeleton with streams groupingBy (the OSM link id is a plain String for now).
TESTS: table-driven dedup scenarios (same place + similar, same place + different, far apart, low accuracy widening, manual pin, similarity unavailable -> NEEDS_REVIEW, different families),
 best-view cases, severity boundaries (below/at/above each threshold), union-area properties. Use FakeDetector outputs and generated images, never real photos.
RULES: no persistence, no UI; every threshold from config; mvn verify green; record dependencies.
OUTPUT: class diagram (Mermaid) saved in docs/uml/analysis.md, files, commands + output, decisions for me.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 5. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-05.md.
Checks:
1. `mvn -B verify` green; quote test counts per package and JaCoCo coverage for analysis.
2. Map every branch of design 5.3 (the four rules) to at least one test; list the test names. Missing branch = FAIL.
3. Doubt case: construct a scenario (similarity unavailable) and show the result is NEEDS_REVIEW, not a merge and not a split.
4. Accuracy widening and the cap: show tests at exactly the boundary distance (inside, on, outside).
5. SpatialIndex results equal brute force on a randomised test (seeded). Quote the test.
6. Union-area property tests exist and pass (list properties). Manually verify one overlapping-box example by hand arithmetic in the report.
7. Severity: boundary tests at each threshold; thresholds read from config only (grep for numeric literals in AreaRatioSeverity).
8. No wording or code claims real-world size, depth or volume (grep "depth|volume|cm|mm" in analysis/domain).
9. thresholds.yaml severity values match what the user supplied (ask the user to confirm; mark NOT RUN if unconfirmed). All new thresholds still say UNVALIDATED.
10. domain remains free of I/O imports; no references to video/track.
11. New dependencies in docs/DEPENDENCIES.md with licence (OpenCV or fallback).
Verdict + defects.
````

## EXPECTED OUTPUTS
- Classes above, tests, `docs/uml/analysis.md`, severity values in `thresholds.yaml`.
- Commands: `mvn -B verify` green. Typical test groups: dedup (>= 8 scenarios), best view, severity boundaries, union-area properties, spatial index vs brute force.
- Pass criteria: every dedup rule branch tested; default-to-review proven; no unvalidated threshold promoted.

## Commit / PR
`feat(analysis): defect assembly, dedup, best view, severity, union area`

---

# Phase 6: Priority + OSM road links
**Branch:** `phase/06-priority-osm` | **Builder:** Claude Sonnet 5.5 | **Verifier:** Gemini 3.1 Pro | **Quota:** medium | **Depends on:** Phase 5

## Goal
Explainable priority (severity x road weight x exposure x recurrence, plus optional corroboration) and snapping defects to cached OSM road links, with no network at runtime.

## You do (manual)
Provide the priority formula weights from the main document (road-class weights, exposure scale, recurrence step) or approve starting values labelled UNVALIDATED.
Approve one-time OSM download of a Chennai area extract (Overpass or Geofabrik) via a script; attribution (ODbL) goes in the README and the UI.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (5.8, 5.9), thresholds.yaml. PHASE 6: priority + OSM. Java; one small Python/shell script allowed for fetching OSM data.
STEP 0: priority weights in thresholds.yaml are null. Ask me for the values or for approval of starting values (they must stay marked UNVALIDATED).
STEP 1: Implementation Plan, wait for approval.
STEP 2:
 - com.roadai.priority: PriorityFactor interface {String name(); double value(Defect, Context)}, SeverityFactor, RoadClassFactor, ExposureFactor, RecurrenceFactor, CorroborationFactor (optional, number of independent reports).
   PriorityCalculator multiplies factors and returns PriorityResult {score, List<FactorContribution(name, value)>}. Adding a factor must not require editing existing classes (prove with a test that registers a new factor).
 - OsmContext interface + CachedOsmContext reading a local GeoJSON/JSON snapshot of road links (id, highway class, geometry). Never calls the network at runtime.
 - scripts/fetch_osm.py (or .sh): one-time download for a bounding box given as arguments; records source, query, date, and ODbL attribution into data/osm/README.md. Ask me before running; do not commit the extract.
 - geo.RoadLinkSnapper: nearest road link using SpatialIndex and point-to-segment distance (state the approximation, e.g. local equirectangular), max snap distance from config, unsnappable -> "no link" (not an error).
 - LinkSummaryBuilder completed: defects per link via groupingBy, ordered. No Good/Fair/Poor in spot mode (design 5.8).
 - PriorityStabilityTest: perturb weights (seeded, stated perturbation range), compute the share of top-N that stays in top-N, write the result to docs/priority_stability.json (the test or a script writes it; it must not be hand-typed).
 - Defects with no location must be excluded from the ranked map queue and listed as LOCATION_NEEDED.
TESTS: factor math with hand-computed examples; zero/negative guards; snapping on a tiny synthetic road network fixture (crossroads, parallel roads, far point); stability test deterministic with a seed.
OUTPUT: files, commands + output, the stability JSON, assumptions.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 6. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-06.md.
Checks:
1. `mvn -B verify` green; test counts; coverage for priority and geo.
2. Recompute two PriorityResult examples by hand and compare with the code's output (show arithmetic).
3. Add-a-factor test exists and shows no edit to existing factor classes is needed.
4. Snapper: crossroads, parallel roads and far-point tests exist; unsnappable returns an explicit "no link" value; the max snap distance comes from config.
5. CachedOsmContext makes no network calls (grep java.net / HttpClient in the class and its package; run its tests offline).
6. Fetch script: records source/date/attribution; data/osm not tracked by git; README/UI attribution text for ODbL present.
7. Stability: run the test twice; same seed gives the same docs/priority_stability.json; JSON is script-written; RESULTS.md still says NOT YET MEASURED unless make_results maps it (check which).
8. Spot mode produces defects-per-link only, no condition rating.
9. Unlocated defects are excluded from the ranked queue (test evidence).
10. Weights in thresholds.yaml carry UNVALIDATED comments unless the user supplied validated ones.
Verdict + defects.
````

## EXPECTED OUTPUTS
- Priority classes + tests, `CachedOsmContext`, `RoadLinkSnapper`, `scripts/fetch_osm.*`, `docs/priority_stability.json`.
- Pass criteria: hand-checked math, open-for-extension proven, offline runtime, ODbL attribution present.

## Commit / PR
`feat(priority): explainable priority and OSM road-link snapping`

---

# Phase 7: Persistence, roles, pipeline
**Branch:** `phase/07-persistence-roles` | **Builder:** plan Claude Opus 5.5, build Claude Sonnet 5.5 | **Verifier:** Gemini 3.1 Pro | **Quota:** heavy (plan), medium (build) | **Depends on:** Phases 4-6

## Goal
Everything survives a restart, every change is audited, roles gate actions, and `ReportPipeline` ties phases 4-6 together with batch uploads.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (4, 5.2, 5.7, 5.10, 10), docs/ARCHITECTURE.md. PHASE 7: persistence + roles + pipeline. Java only.
STEP 1 (use your strongest reasoning): Implementation Plan with the SQLite schema (reports, observations, defects, verifications, audit, users, schema_version), indexes, transaction boundaries, the pipeline stage list, and the concurrency plan. Wait for approval.
STEP 2:
 - persistence: Repository<T,ID> generic interface; ReportRepository, DefectRepository, VerificationRepository, AuditRepository; SqliteDatabase (JDBC, candidate driver: verify licence), versioned migrations (hand-rolled or a library; record in DEPENDENCIES.md),
   PreparedStatements only, JDBC transactions where multi-row consistency matters, in-memory DB for tests.
 - Audit: every state change writes (who, when, from, to, note) in the same transaction as the change. Audit rows are append-only (no update/delete API).
 - security: abstract User; Surveyor, Engineer, Admin with permissions() returning a Permission set; a PermissionGuard used by services (e.g. only Engineer/Admin can set workflow states; only Admin manages users/thresholds).
   Passwords: if you add local login, use a vetted password-hashing approach (verify library) and never store plaintext; otherwise document that identity is a selected role for this prototype (state it plainly as a limitation).
 - config: Thresholds immutable, loaded once, validated (null/missing keys -> clear error).
 - service: ReportPipeline as a Template Method with the fixed stage order from design 5.2 (hooks overridable for the survey-mode pipeline later); ReportBuilder; BatchUploadService using ExecutorService/CompletableFuture with ProgressListener (Observer).
   One failing photo must not abort the batch; failures are recorded per file with a reason. Pipeline order: EXIF first, then mark image for stripped storage, quality gates (flag), detect, location, redact placeholder (real redaction in Phase 10 but the stage exists), observations, assembler, best view, severity, priority, snap, persist + audit.
 - Idempotency: the same file hash uploaded twice does not create duplicate reports (return the existing one, record an audit note).
TESTS: repository CRUD with in-memory DB; transaction rollback leaves no partial rows; audit append-only; permission matrix table-driven; pipeline end-to-end with FakeDetector and generated images (merge case, review case, no-location case, quality-flag case);
 concurrency test with N parallel uploads yields consistent counts; duplicate-hash upload; migration from empty DB.
RULES: no UI code; no network; no real photos; mvn verify green.
OUTPUT: schema SQL, class diagram in docs/uml/persistence.md, commands + output, decisions for me.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 7. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-07.md.
Checks:
1. `mvn -B verify` green; counts and coverage for persistence, security, service.
2. Schema file matches docs/ARCHITECTURE.md table list; indexes exist for location/defect lookups; no string-concatenated SQL (grep for "+ " inside SQL strings and Statement usage).
3. Rollback test: show a forced failure mid-transaction leaves zero partial rows.
4. Audit is append-only: no update/delete methods or SQL on the audit table (grep); each transition test shows exactly one audit row with who/when/from/to/note.
5. Permission matrix: list every (role, action) tested; Surveyor cannot set workflow state; Engineer cannot manage users/thresholds.
6. Pipeline: stage order equals design 5.2 (show the template method); end-to-end tests cover merge, NEEDS_REVIEW, no-location, quality-flag cases.
7. Batch: inject one failing file; the others succeed and the failure is reported; ProgressListener received events (test evidence).
8. Concurrency test is deterministic enough to be reliable (run it 5 times; report any flake).
9. Duplicate file hash: no duplicate report; audit note present.
10. Thresholds fail-fast on missing keys (test evidence). No passwords stored in plaintext (grep and schema).
11. Domain has no JDBC imports; ui untouched.
Verdict + defects.
````

## EXPECTED OUTPUTS
- Schema + migrations, repositories, audit, roles, `ReportPipeline`, `BatchUploadService`, tests, `docs/uml/persistence.md`.
- Pass criteria: transactional integrity proven, audit append-only, permissions enforced in services (not only the UI), batch resilient.

## Commit / PR
`feat: sqlite persistence, audit, roles, report pipeline and batch upload`

---

# Phase 8: JavaFX UI (survey mode is second priority)
**Branch:** `phase/08-ui` | **Builder:** Claude Sonnet 5.5 (Gemini 3.1 Pro if Claude is locked) | **Verifier:** Gemini 3.1 Pro + your manual checklist | **Quota:** medium-heavy | **Depends on:** Phase 7

## Goal
A usable desktop app over the existing services: upload, map, ranked queue with priority breakdown, defect gallery, review of ambiguous merges, "location needed" list. UI holds no domain logic.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (4, 5.5, 5.7, 5.8, 10), docs/ARCHITECTURE.md. PHASE 8: UI. Java 21 + JavaFX. Image-only.
STEP 1: Implementation Plan: screen list, view-model list, navigation, how the map works offline and online (candidate approaches: a JavaFX WebView with Leaflet and OSM tiles, or a library; verify licences. OSM tile usage policy and attribution
  must be respected; add a clearly labelled offline fallback that draws points on a plain coordinate canvas). Wait for approval.
STEP 2: com.roadai.ui with MVVM: views (FXML or code) + view-models; view-models call services only; no SQL, no thresholds, no severity logic in ui.
 Screens (priority order):
  1. Upload: pick files or folder, batch progress (ProgressListener), per-file result (accepted / flagged with reasons / failed), manual map-pin for photos without location.
  2. Ranked queue: table sorted by priority, a column with the factor breakdown (tooltip or expandable row), filters by severity, class, machine/workflow state, road link; defects with no location shown in a separate "Location needed" list.
  3. Map: defect markers colour-coded by severity, click -> defect panel; location_source shown, MANUAL_PIN flagged visibly.
  4. Defect detail/gallery: best-view image with box overlay, all observations, why merged (distance/similarity numbers), NEEDS_REVIEW merge/split buttons (Engineer/Admin only), audit timeline.
  5. Admin: users/roles, thresholds viewer (read-only unless Admin), data retention/delete actions (privacy).
  6. Always-visible banner when running on demo data: "SYNTHETIC LOCATIONS / DEMO DATA" (a data flag decides it, not a manual switch).
  7. Survey mode (second priority; implement only after 1-6 are done and I say so).
 Permissions: controls hidden or disabled by PermissionGuard results, never by role-name string checks in the ui.
 Accessibility: keyboard navigation for the queue, sufficient contrast, no colour-only meaning.
TESTS: view-model unit tests (sorting, filtering, location-needed split, banner flag, permission-driven enablement). UI smoke test with TestFX or similar only if verified and stable; otherwise a manual checklist at docs/UI_CHECKLIST.md with expected results per screen.
OUTPUT: files, commands + output, the manual checklist, screenshots path (I will capture them), decisions for me.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 8. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-08.md. (You cannot see the running UI: static checks + tests; leave runtime items to the human checklist.)
Checks:
1. `mvn -B verify` green; view-model test list and counts.
2. ui package: grep for java.sql, SQL strings, Thresholds numeric usage, severity/priority calculations. Any hit = FAIL.
3. Permissions: ui uses PermissionGuard results; grep for role-name string comparisons.
4. Banner is driven by a data flag; test evidence.
5. "Location needed" list is separate from the ranked queue (test evidence).
6. Map: OSM attribution is shown; tile usage respects the policy (no bulk prefetching); an offline fallback exists.
7. docs/UI_CHECKLIST.md covers every screen with concrete steps and expected results; list missing ones.
8. Each NEEDS_REVIEW action goes through a service call that writes an audit row (trace the call path).
9. Large queue (e.g. 5,000 synthetic defects): code uses virtualised table/lazy loading, no per-row blocking I/O on the FX thread (inspect; list any FX-thread blocking calls).
10. No hard-coded absolute paths; dependencies and licences recorded.
Human checklist to run afterwards (report as NOT RUN until you do): launch the app, upload 5 generated images, merge a NEEDS_REVIEW pair, switch roles.
Verdict + defects.
````

## EXPECTED OUTPUTS
- `ui` package, view-models + tests, `docs/UI_CHECKLIST.md`, screenshots you add under `docs/img/`.
- Run: `mvn javafx:run` (or the documented command) opens the app.
- Pass criteria: zero domain logic in ui, permission-driven controls, audit on every review action, banner works.

## Commit / PR
`feat(ui): upload, ranked queue, map, defect detail, admin`

---

# Phase 9: Proof-of-visit verification + state machines
**Branch:** `phase/09-verification` | **Builder:** plan Claude Opus 5.5, build Claude Sonnet 5.5 | **Verifier:** Gemini 3.1 Pro | **Quota:** heavy (plan), medium (build) | **Depends on:** Phases 5-8

## Goal
A defect never becomes "fixed" because it was missing from a photo. After photos pass explicit checks, an engineer signs off, and recurrences are caught.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (5.6, 5.7, 5.9), thresholds.yaml (verify.*). PHASE 9: verification + lifecycle. Java.
STEP 1 (use your strongest reasoning): Implementation Plan with BOTH state machines as tables (state, event, guard, next state, audit note), every illegal transition, and the check flow. Wait for approval.
STEP 2:
 - lifecycle: enums MachineState {NEW, REPORTED_AGAIN, NEEDS_REVIEW, AFTER_PHOTO_CLEAR, AFTER_PHOTO_STILL_DAMAGED, AFTER_PHOTO_REJECTED, REAPPEARED} and WorkflowState {UNREVIEWED, CONFIRMED, REJECTED, REPAIR_ORDERED, REPAIR_IN_PROGRESS, REPAIRED_CLAIMED, VERIFIED_FIXED}
   exactly as design 5.7; DefectStateMachine (State pattern) enforcing legal transitions; IllegalTransitionException; every transition writes an audit row in the same transaction.
 - ProofOfVisitChecker with one Strategy/check per row of design 5.6: time (after repair-claimed date; label EXIF time as evidence, not proof), location (within visit_radius_m, accuracy considered), scene match (ImageSimilarity >= min_scene_similarity),
   quality (QualityGate + defect area plausibly in frame), detector (no same-family damage at >= clear_conf; patch class not counted as damage). The result lists EVERY failed check with reasons.
 - VerificationService: AFTER_PHOTO_CLEAR / AFTER_PHOTO_STILL_DAMAGED / AFTER_PHOTO_REJECTED; VERIFIED_FIXED only via an Engineer/Admin sign-off with a mandatory note (guard: requires AFTER_PHOTO_CLEAR or an explicit override note).
   A later report at the same place after VERIFIED_FIXED sets REAPPEARED and increases the recurrence factor (use the existing RecurrenceFactor).
 - Wire into ui only through existing view-models: a "Submit after photo" action in the defect detail screen and the sign-off dialog (Engineer/Admin only).
TESTS: exhaustive transition table test (every state x event; legal -> expected state, illegal -> exception); each check has pass and fail cases with FakeDetector and generated images; combined multi-failure result; sign-off without note rejected;
 Surveyor cannot sign off; reappearance raises priority (compute before/after); EXIF time edited case documented as limitation in a test name/comment; audit rows counted per transition.
RULES: no staged data presented as real; mvn verify green.
OUTPUT: state tables saved in docs/uml/lifecycle.md (Mermaid stateDiagram), files, commands + output.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 9. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-09.md.
Checks:
1. `mvn -B verify` green; counts and coverage for lifecycle.
2. Enum constants equal design 5.7 exactly (diff the lists); no extra or missing states.
3. Exhaustive transition test: show its size (states x events) and that illegal transitions throw IllegalTransitionException.
4. One test per ProofOfVisit check (pass + fail) as in design 5.6; the multi-failure test lists all reasons.
5. A defect can reach VERIFIED_FIXED only via sign-off with a note; try to bypass it through every public API (list the attempts and the outcomes).
6. Role enforcement: Surveyor sign-off denied (test evidence).
7. Reappearance: show priority before and after with real numbers from the test.
8. Each transition writes exactly one audit row in the same transaction (rollback test).
9. "Absence from a photo" never triggers fixed: grep for any logic that marks fixed from a detection-free report; show the test.
10. UML state diagrams exist in docs/uml/lifecycle.md and match code.
Verdict + defects.
````

## EXPECTED OUTPUTS
- Lifecycle package, checker + service, UI hooks, tests, `docs/uml/lifecycle.md`.
- Pass criteria: no path to VERIFIED_FIXED without sign-off; all failed checks reported; every transition audited.

## Commit / PR
`feat(lifecycle): proof-of-visit verification and state machines`

---

# Phase 10: Hardening (privacy, agreement metrics, stability, ONNX gate)
**Branch:** `phase/10-hardening` | **Builder:** Claude Opus 5.5 for the security/privacy review, Claude Sonnet 5.5 for fixes | **Verifier:** Gemini 3.1 Pro | **Quota:** heavy | **Depends on:** Phases 1-9

## Goal
Close the honesty gaps: privacy actually works, agreement and stability numbers exist with provenance, and the ONNX decision gate is resolved on evidence.

## You do (manual)
Collect expert severity ratings (rating sheet CSV), labelled same/different defect photo pairs, and a few real before/after pairs if available. The agent builds tools and analysis scripts; it must not invent any of this data.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (5.10, 8, 12, 13), docs/RESULTS.md. PHASE 10: hardening. Both languages.
STEP 1: Implementation Plan, wait for approval. Start with a written security + privacy review of the current code (threat list, findings table, severity), then fix.
STEP 2:
 1. Privacy: ExifStripper (read EXIF first, store stripped), Redactor interface + BlurRedactor for faces and plates in stored images and crops (candidate detectors: verify licences; default to blurring MORE not less);
    export paths also redact; retention + delete options work end to end (images, crops, DB rows). Tests on generated images with synthetic "plate-like" and "face-like" regions as far as feasible; state clearly what is NOT covered
    and add a manual redaction check to docs/PRIVACY_CHECKLIST.md. Do not claim a detection rate until measured on real samples; write NOT YET MEASURED.
 2. Upload security: size/type validation in Java too, no path traversal from file names, temp-file cleanup, request timeouts, API key never logged. Add tests.
 3. scripts/kappa.py: Cohen's kappa (and weighted kappa) between model severity and expert ratings from a CSV rating sheet; writes docs/severity_kappa.json with n, kappa, confusion table. Provide a CSV template and unit tests with a hand-computed example.
 4. Quality-gate agreement tool: compares gate flags with human judgement CSV; writes docs/quality_gate_agreement.json.
 5. Dedup evaluation tool: reads labelled same/different pairs CSV, runs the assembler's similarity + distance rules, writes docs/dedup_eval.json (precision, recall, confusion). Sweep thresholds on a TUNING subset and report on a held-out subset; never report tuned-on numbers as final.
 6. Proof-of-visit evaluation tool on real pairs (if present): writes docs/proof_of_visit_eval.json.
 7. Benchmark: upload-to-ranked-ticket time over N generated images (stopwatch in code, hardware recorded) -> docs/benchmark.json.
 8. Coverage: record mvn verify test count + JaCoCo line coverage into docs/test_summary.json via script.
 9. Extend scripts/make_results.py to map every JSON above into RESULTS.md; missing -> NOT YET MEASURED. Re-run priority stability from Phase 6.
 10. ONNX decision gate (design 8): ONLY run if Phases 4-9 are verified. Implement OnnxDetector behind DamageDetector (letterboxing, decoding, NMS in Java), run the parity set against RemoteYoloDetector, write docs/onnx_parity.json
     (box/class/confidence deltas, speed, platform). Gate result = pass/fail by criteria you propose and I approve beforehand. If it fails or I decline, keep RemoteYoloDetector and delete nothing else.
 11. Dependency licence audit: fill docs/DEPENDENCIES.md; flag AGPL/GPL implications for distribution.
RULES: no fabricated data, no tuning on the final test, every number from a JSON.
OUTPUT: review findings table, files, commands + output, JSON paths, gate decision.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 10. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-10.md.
Checks:
1. `mvn -B verify` and `pytest -q` + `ruff check .` green. Quote counts.
2. Privacy: stored images contain no EXIF (inspect a stored file with an EXIF tool or code); redaction is applied on storage AND export paths (trace both). Retention/delete removes files and DB rows (test evidence).
3. Upload security tests: oversize, wrong type, `../` file names, zero-byte, corrupt JPEG. API key absent from logs (run and grep).
4. kappa.py: verify the unit test with a hand-computed example and recompute kappa independently on the sample CSV (show the arithmetic).
5. Dedup tool: confirm the tuning/held-out split is separate and the reported numbers are from held-out data.
6. Every JSON referenced by RESULTS.md exists and the numbers match; list any number without a source. Missing data must read NOT YET MEASURED (not 0, not blank).
7. Benchmark JSON includes hardware, N, and method; numbers reproducible within a stated tolerance (re-run).
8. ONNX gate: either docs/onnx_parity.json exists with the approved criteria and a clear pass/fail, or the report shows the gate was skipped by decision. No half state.
9. docs/DEPENDENCIES.md has a licence for each dependency; AGPL/GPL implications are called out.
10. Honesty sweep: grep the repo for unlabelled synthetic/staged data and for percentages/mAP/kappa values in docs not produced by scripts.
Verdict + defects.
````

## EXPECTED OUTPUTS
- `docs/severity_kappa.json`, `quality_gate_agreement.json`, `dedup_eval.json`, `proof_of_visit_eval.json`, `benchmark.json`, `test_summary.json`, `priority_stability.json`, optionally `onnx_parity.json`; `docs/PRIVACY_CHECKLIST.md`; updated `RESULTS.md`.
- Pass criteria: every row in RESULTS.md has a script-written source or says NOT YET MEASURED; privacy verified on storage and export; gate decided on evidence.

## Commit / PR
`feat: privacy, evaluation tools, benchmarks and onnx decision gate`

---

# Phase 11: Demo, UML, OOP document, report
**Branch:** `phase/11-demo-report` | **Builder:** Gemini 3.1 Pro (writing) + Gemini 3.8 Flash (UML/boilerplate) | **Verifier:** Claude Sonnet 5.5 | **Quota:** medium | **Depends on:** Phase 10

## Goal
A reproducible demo with honest labels, complete UML and OOP mapping, and a report that quotes only measured numbers.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (4.2, 10, 11, 12, 13, 14), docs/RESULTS.md. PHASE 11: demo + documentation. Do not add features.
STEP 1: Implementation Plan, wait for approval.
STEP 2:
 1. ai-service/scripts/make_demo_data.py: takes real RDD2022 TEST-split images (the labelled grouped split) and assigns SYNTHETIC coordinates along a made-up route in Chennai and synthetic report dates; creates before/after pairs ONLY from user-provided pairs, otherwise
    clearly labelled staged pairs. Output to sample-data/demo/ (check the dataset licence before committing any image; if unsure keep images out of git and commit only the generator + a manifest). Manifest flags every item SYNTHETIC_LOCATION / STAGED. The app's banner must trigger from this flag.
 2. docs/DEMO_PLAYBOOK.md final: live mode (own Chennai photos with EXIF through the real model; quote only RESULTS.md numbers) and offline mode (banner always on), with the story: several reporters -> duplicates merge -> ranked queue -> after photo passes proof-of-visit -> repaired defect reappears and priority rises. Step-by-step click script with expected screen states.
 3. docs/uml/: class diagrams per package, sequence diagram of ReportPipeline, state diagrams (from Phase 9), component diagram (Python service, Java engine, SQLite, OSM cache). Diagrams generated from the real code structure (verify they match class names).
 4. docs/OOP_DESIGN.md: complete the design 4.2 table with real class names and file paths, and add two short code excerpts per major pattern. Include a section "Rubric mapping" with placeholders for the supervisor's marking scheme.
 5. docs/REPORT.md (or the format I specify): sections: problem, design decisions (image-only ADR), architecture, data and its caveats (D40, labelling, spot bias, EXIF), methods, results (ONLY from RESULTS.md; NOT YET MEASURED rows stay as such), limitations (design section 12 + what you found), ethics/privacy, future work.
 6. README.md final: accurate status, quickstart that works from a clean clone (run it), screenshots, licence section (confirm with me which licence), dataset and OSM attribution.
 7. scripts/honesty_check.py: scans docs/ for numbers near metric words (mAP, precision, recall, kappa, %, ms, tests) that do not appear in any docs/*.json; exits non-zero on findings. Add it to CI.
 8. Tag v0.1.0 instructions in CHANGELOG (do not push tags yourself).
OUTPUT: files, commands + output (including a clean-clone run), honesty_check output.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 11. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-11.md.
Checks:
1. Clean clone test: clone into a temp folder, follow README quickstart exactly, report which steps fail.
2. `mvn -B verify`, `pytest -q`, `ruff check .`, `python scripts/honesty_check.py` all succeed.
3. Run make_demo_data.py; manifest flags every item; banner triggers in the app data flag path (code trace).
4. docs/REPORT.md: every number maps to a JSON (list the mapping); NOT YET MEASURED rows preserved; limitations section includes EXIF issues, spot bias, D40 caveat, single-frame false positives, staged pairs.
5. docs/OOP_DESIGN.md: each class/path in the table exists (script check); each pattern has an excerpt that matches the real code.
6. UML class names exist in code (script check or list mismatches).
7. DEMO_PLAYBOOK steps are executable (follow 5 steps and report the screen states you can verify statically; mark UI-runtime items NOT RUN).
8. Licence and attribution: dataset (RDD2022), OSM ODbL, third-party libs. List gaps. Confirm no dataset images are committed unless their licence allows it.
9. No video/tracker wording outside ADR 0001 and the design document.
10. CHANGELOG updated; no stray large files.
Verdict + defects.
````

## EXPECTED OUTPUTS
- `make_demo_data.py` + manifest, `DEMO_PLAYBOOK.md`, UML set, `OOP_DESIGN.md`, `REPORT.md`, final `README.md`, `scripts/honesty_check.py` wired into CI.
- Pass criteria: clean-clone quickstart works; honesty check clean; every report number traceable; synthetic/staged data labelled.

## Commit / PR
`docs: demo, uml, oop design, report and honesty check` -> tag `v0.1.0` yourself after merge.

---

