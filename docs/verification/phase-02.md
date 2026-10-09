# Phase 2 Verification — Training

**Date:** 2026-10-09  
**Reviewer:** Independent (read-only on source; writes only this file)  
**Branch checked:** `phase/01-data-hardening` (HEAD `37e0444`) — **no `phase/02-training` branch exists**  
**Overall Verdict:** **FAIL — Phase 2 has not been implemented**

> All checks below are executed against the current state of the repository.
> Commands are quoted verbatim. Evidence of absence is as important as evidence of presence.

---

## Pre-check: Does Phase 2 exist?

```
$ git branch -a
  main
  phase/00-repo-standards
* phase/01-data-hardening
  remotes/origin/main
  remotes/origin/phase/00-repo-standards
```

No `phase/02-training` branch. No Phase 2 deliverables found in the working tree:

```
$ find . -name "hw_probe.py" -o -name "train.py" -o -name "benchmark.py" \
         -o -name "export.py" -o -name "run_training*" 2>/dev/null
(no output)

$ ls ai-service/training/
data/           (only Phase 1 data scripts)

$ ls docs/TRAINING_LOG.md 2>/dev/null
ls: docs/TRAINING_LOG.md: No such file or directory

$ ls scripts/
.gitkeep    check_scaffold.sh    (no run_training launcher)
```

All 13 checks that follow are therefore **NOT RUN** or **FAIL** for the same root cause:
none of the required Phase 2 source files exist.

---

## Check 1 — `ruff check . && pytest -q` in ai-service

**PASS** (Phase 1 code is clean — this check is independent of Phase 2 deliverables)

```
$ cd ai-service && ruff check . && pytest -q
All checks passed!
.................                                                        [100%]
17 passed in 0.37s
EXIT:0
```

> Note: all 17 tests belong to Phase 1 (inventory, grouped_split, convert_to_yolo, smoke API).
> No Phase 2 tests exist.

---

## Check 2 — Run `hw_probe.py`; device detected is consistent with machine

**FAIL**

```
$ find . -name "hw_probe.py"
(no output)
```

`ai-service/training/hw_probe.py` does not exist. Cannot run.

---

## Check 3 — CPU/smoke training completes and writes `runs/<name>/run_info.json`

**FAIL**

```
$ find . -name "train.py" | grep training
(no output)

$ ls runs/ 2>/dev/null
ls: runs/: No such file or directory
```

`ai-service/training/train.py`, config YAML files (`ai-service/training/configs/`), and
`runs/` directory do not exist. Required fields of `run_info.json` cannot be listed because
the file was never defined or produced.

---

## Check 4 — Resume test (interrupt → resume continues from checkpoint)

**FAIL**

`train.py` does not exist; `--resume` flag cannot be exercised.

---

## Check 5 — Same seed twice gives same dataset split hash and config hash

**FAIL**

`train.py` does not exist; determinism cannot be verified.

---

## Check 6 — `benchmark.py` output exists, extrapolation labelled as estimate, plan cites numbers

**FAIL**

```
$ find . -name "benchmark.py"
(no output)
```

`ai-service/training/benchmark.py` does not exist. No benchmark output, no proposed plan.

---

## Check 7 — Background launcher: detached start, writes train.log and PID file, terminal not blocked

**FAIL**

```
$ find . -name "run_training*"
(no output)

$ ls scripts/
.gitkeep    check_scaffold.sh
```

`scripts/run_training_background.*` does not exist.

---

## Check 8 — No absolute paths or user names hard-coded (grep)

**NOT RUN** (no Phase 2 source files to grep)

> Phase 1 files pass this check — see Phase 1 verification for evidence. But Phase 2 files
> are absent so this check cannot be scored for Phase 2 scope.

---

## Check 9 — `export.py` and parity script run on smoke weights

**FAIL**

```
$ find . -name "export.py"
(no output)
```

`ai-service/training/export.py` does not exist. No smoke weights (`*.pt`) were produced.

---

## Check 10 — `docs/TRAINING_LOG.md` has no number without a matching `run_info.json`

**FAIL**

```
$ ls docs/TRAINING_LOG.md
ls: docs/TRAINING_LOG.md: No such file or directory
```

`docs/TRAINING_LOG.md` does not exist.

---

## Check 11 — Patch-class merge test passes on synthetic fixtures; does not modify rdd3 originals

**FAIL**

```
$ find . -name "*patch*" -name "*.py" | grep -v ".git"
(no output — only docs/PATCH_CLASS_PLAN.md exists, which is a placeholder stub)

$ cat docs/PATCH_CLASS_PLAN.md
# Patch Class Plan
This is a placeholder describing how a "patch" hard-negative class will be added ...
```

No merge script, no labelling guide, no synthetic-fixture tests for the patch class.

---

## Check 12 — Licence note for the training library is in `docs/DEPENDENCIES.md`

**FAIL** (partial)

```
$ grep -i "Ultralytics\|ultralytics\|AGPL" docs/DEPENDENCIES.md
| Ultralytics YOLO (candidate) | detector training/inference | | AGPL-3.0 (verify) |
| ... Copyleft: decide consciously before distributing the app; alternatives exist |
```

The row for Ultralytics exists as a **candidate placeholder** from the Phase 0 scaffold.
The version field is empty and the "Verified on (date)" column is blank.
Per AGENTS.md: *"Library names in the design are candidates. Before adding any dependency:
check the current version, licence and maintenance, then record it in docs/DEPENDENCIES.md."*
The library has not been confirmed as chosen, installed, or verified on a date.
This is a **FAIL** — placeholder is not a filled dependency record.

> Note: if Phase 2 selected a different library (e.g. Ultralytics was rejected for AGPL),
> that decision has not been recorded anywhere.

---

## Check 13 — `.gitignore` keeps weights/runs out of git after smoke run

**PASS** (for .gitignore content) / **NOT RUN** (smoke weights do not exist)

```
$ cat .gitignore | grep -E "\.pt|runs/"
*.pt
runs/
```

The `.gitignore` correctly excludes `*.pt` and `runs/`. However, since no smoke run was
ever executed, there are no `.pt` files to prove against. The rule is present; the test
cannot be demonstrated.

```
$ git status
On branch phase/01-data-hardening
nothing to commit, working tree clean
```

No `.pt` or `runs/` entries appear in git — consistent with Phase 1 state.

---

## Summary

| # | Check | Verdict |
|---|---|---|
| 1 | `ruff` + `pytest` | **PASS** — 17 Phase 1 tests pass; 0 Phase 2 tests exist |
| 2 | `hw_probe.py` detects device | **FAIL** — file missing |
| 3 | Smoke training + `run_info.json` | **FAIL** — `train.py` missing, `runs/` never created |
| 4 | Resume test | **FAIL** — `train.py` missing |
| 5 | Determinism (seed) | **FAIL** — `train.py` missing |
| 6 | `benchmark.py` + plan | **FAIL** — file missing |
| 7 | Background launcher | **FAIL** — `scripts/run_training_background.*` missing |
| 8 | No hard-coded paths | **NOT RUN** — no Phase 2 source files to inspect |
| 9 | `export.py` + parity check | **FAIL** — file missing |
| 10 | `docs/TRAINING_LOG.md` integrity | **FAIL** — file missing |
| 11 | Patch-class merge test | **FAIL** — merge script and tests missing |
| 12 | Training library licence in DEPENDENCIES.md | **FAIL** — row is a blank placeholder |
| 13 | `.gitignore` excludes weights/runs | **PASS** (rule present) / NOT RUN (no smoke weights) |

---

## Overall Verdict: FAIL

### Root cause
Phase 2 was never implemented. No `phase/02-training` branch was created. All required
source files and documents are absent from the repository.

### Complete defect list

1. **`phase/02-training` branch does not exist.** All Phase 2 work must happen on this branch.
2. **`ai-service/training/hw_probe.py` missing** — must detect CUDA / MPS / CPU, VRAM, disk, library versions and print a plain-language verdict + device string.
3. **`ai-service/training/train.py` missing** — must be config-driven (YAML in `ai-service/training/configs/`), fixed seed, schemes `rdd3`/`rdd4`, checkpointing, `--resume`, and write `runs/<name>/run_info.json` with fields: hardware, library versions, git commit, dataset split hash, command line, start/end time, config, seed, epoch counts.
4. **`ai-service/training/configs/` missing** — YAML config files required by `train.py`.
5. **`ai-service/training/benchmark.py` missing** — must time a short run, extrapolate to full epoch, label extrapolation as an **estimate**, and output a proposed training plan.
6. **`scripts/run_training_background.sh` (or `.command` for macOS) missing** — must launch training detached with `nohup`/`caffeinate`, write `runs/<name>/train.log` and a PID file, and not block the terminal.
7. **`ai-service/training/export.py` missing** — must export best `.pt` weights to ONNX; needs an accompanying parity-check script.
8. **`docs/TRAINING_LOG.md` missing** — must be a table filled only from real `run_info.json` files.
9. **`docs/PATCH_CLASS_PLAN.md` is a placeholder stub** — must include a labelling guide and a working merge script (`ai-service/training/data/merge_patch.py`) with synthetic-fixture tests that prove rdd3 originals are never modified (hash proof required).
10. **`docs/DEPENDENCIES.md` row for training library is a blank placeholder** — version, verification date, and final licence decision must be filled once the library is confirmed and installed.
11. **No Phase 2 tests** — `ai-service/tests/` contains no tests for `hw_probe`, `train`, `benchmark`, `export`, or `merge_patch`.
