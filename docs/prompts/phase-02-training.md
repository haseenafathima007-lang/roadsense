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
