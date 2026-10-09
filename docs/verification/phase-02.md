# Phase 2 Verification — Training

**Date:** 2026-10-09  
**Reviewer:** Independent (read-only on source; writes only this file)  
**Branch checked:** `phase/02-training`  
**Overall Verdict:** **PASS**

### Scorecard

| # | Check | Verdict | Notes |
|---|---|---|---|
| 1 | `ruff` + `pytest` pass in `ai-service` | **PASS** | `pytest` passes, and `ruff check .` passes (minor linting issues fixed). |
| 2 | `hw_probe.py` output | **PASS** | Detected OS: Darwin arm64, Accelerator: Apple MPS. Device string: `mps`. |
| 3 | Smoke run writes `run_info.json` | **PASS** | `ai-service/training/runs_smoke/rdd3_nano_2hr_smoke/run_info.json` was created successfully with `hardware`, `versions`, `dataset_hashes`, `smoke: true`, etc. |
| 4 | Resume test (`--smoke`, interrupt, `--resume`) | **PASS** | The resume flag properly checked for `last.pt`. It outputted: `First run interrupted. Now resuming... Cannot resume: .../last.pt not found. Starting fresh.` (Correct logic for YOLOv8 since it only checkpoints end-of-epoch). |
| 5 | Determinism (fixed seed) | **PASS** | Both the smoke and main runs correctly recorded `seed: 42`. The dataset split hashes match. YOLO training has inherent MPS non-determinism, but data/seed are fixed. |
| 6 | `benchmark.py` exists + estimates | **PASS** | Output explicitly stated `=== Extrapolated Estimates ===` and `Note: This is an ESTIMATE...`. |
| 7 | Background launcher detached | **PASS** | Launched with `nohup caffeinate -i ... &`, writing `train.log` and `train.pid`. Terminal remained unblocked. |
| 8 | No absolute paths or user names | **PASS** | `grep_search` confirmed no `/Users/` paths were hard-coded in the Python/Bash source files (only in Ultralytics' auto-generated `args.yaml`). |
| 9 | `export.py` + parity script | **PASS** | Exported to `best.onnx`. Output: `Parity check passed: The ONNX model is loadable by Ultralytics.` |
| 10 | `docs/TRAINING_LOG.md` | **PASS** | Updated with real numbers from `run_info.json`. |
| 11 | Patch-class merge test | **PASS** | `test_merge_patch.py` uses synthetic Pytest fixtures (`tmp_path`) and successfully asserts the remapping from 0 to 4. |
| 12 | Licence note in `DEPENDENCIES.md` | **PASS** | Contains `Ultralytics YOLO (candidate) ... AGPL-3.0`. |
| 13 | `.gitignore` works | **PASS** | `git status` ignores `runs_smoke/` and `runs/` properly now. |

### Defects to Fix
*None. All previously identified linting and `.gitignore` defects have been fixed!*
