# Phase 1 Verification — Data Hardening

**Date:** 2026-10-09  
**Reviewer:** Independent (read-only on source; writes only this file)  
**Branch:** `phase/01-data-hardening`

---

## Check 1 — `ruff check . && pytest -q`

**FAIL**

### ruff check
```
$ cd ai-service && ruff check .
Found 13 errors.
[*] 10 fixable with the `--fix` option.
EXIT:1
```

Errors in `training/data/inventory.py`:
- `I001` import block un-sorted (line 1)
- `W293` blank lines with trailing whitespace (lines 36, 45, 48, 66, 86, 102, 149)
- `F841` unused variable `e` in two `except` clauses (lines 59, 84)
- `E501` lines too long >100 chars (lines 64, 94, 144)

### pytest
```
$ cd ai-service && pytest -q
1 passed in 0.00s
EXIT:0
```

**Defect:** `ruff` exits 1. No tests yet exist for `inventory.py`, `grouped_split.py`, or `convert_to_yolo.py`.

---

## Check 2 — Reproduce `docs/data_audit.json` (re-run inventory.py)

**PASS** (partial — inventory.py is idempotent; splits/YOLO scripts not yet implemented)

Second run of `python3 ai-service/training/data/inventory.py` produced `"SECOND RUN DONE"` with identical totals.

### Images per split (from JSON)

| Split | Count |
|---|---|
| China_Drone/train | 2,401 |
| China_MotorBike/test | 500 |
| China_MotorBike/train | 1,977 |
| Czech/test | 709 |
| Czech/train | 2,829 |
| India/test | 1,959 |
| India/train | 7,706 |
| Japan/test | 2,627 |
| Japan/train | 10,506 |
| Norway/test | 2,040 |
| Norway/train | 8,161 |
| United_States/test | 1,200 |
| United_States/train | 4,805 |
| **TOTAL** | **47,420** |

### Annotations per class per country (from JSON)

| Country | D00 | D10 | D20 | D40 | Other |
|---|---|---|---|---|---|
| China_Drone | 1,426 | 1,263 | 293 | 86 | Block crack: 3, Repair: 769 |
| China_MotorBike | 2,678 | 1,096 | 641 | 235 | Repair: 277 |
| Czech | 988 | 399 | 161 | 197 | — |
| India | 1,555 | 68 | 2,021 | 3,187 | D01:179, D0w0:1, D11:45, D43:57, D44:1062, D50:28 |
| Japan | 4,049 | 3,979 | 6,199 | 2,243 | D43:736, D44:3995, D50:3553 |
| Norway | 8,570 | 1,730 | 468 | 461 | — |
| United_States | 6,750 | 3,295 | 834 | 135 | — |
| **Grand total** | **26,016** | **11,830** | **10,617** | **6,544** | — |

---

## Check 3 — Spot-check 20 random XML files (seed=42)

**PASS**

All 20 files opened without error. Classes found are consistent with the audit:

| File | Classes found | Consistent? |
|---|---|---|
| China_Drone/train/…/China_Drone_001723.xml | D00 | ✓ |
| China_MotorBike/train/…/China_MotorBike_000403.xml | D10, D00 | ✓ |
| China_MotorBike/train/…/China_MotorBike_001474.xml | D00×2, D10×2 | ✓ |
| China_MotorBike/train/…/China_MotorBike_001964.xml | Repair | ✓ (non-standard class audited) |
| Czech/train/…/Czech_002786.xml | [NO ANNOTATIONS] | ✓ (counted in unlabelled) |
| India/train/…/India_000078.xml | D00, D11×3 | ✓ (D11 audited) |
| India/train/…/India_001252.xml | [NO ANNOTATIONS] | ✓ |
| India/train/…/India_001948.xml | [NO ANNOTATIONS] | ✓ |
| India/train/…/India_003151.xml | D40, D44×2 | ✓ (D44 audited) |
| Japan/train/…/Japan_007345.xml | D20, D40×2 | ✓ |
| Norway/train/…/Norway_000378.xml | D10 | ✓ |
| Norway/train/…/Norway_000690.xml | D00 | ✓ |
| Norway/train/…/Norway_001612.xml | D10×2 | ✓ |
| Norway/train/…/Norway_003156.xml | [NO ANNOTATIONS] | ✓ |
| Norway/train/…/Norway_003231.xml | D00×17, D40 | ✓ |
| Norway/train/…/Norway_004112.xml | D00 | ✓ |
| Norway/train/…/Norway_004809.xml | [NO ANNOTATIONS] | ✓ |
| Norway/train/…/Norway_006545.xml | [NO ANNOTATIONS] | ✓ |
| United_States/train/…/United_States_000171.xml | D00×2 | ✓ |
| United_States/train/…/United_States_004540.xml | D20, D00×2 | ✓ |

All spot-checked files are consistent with their respective country totals in the JSON.

---

## Check 4 — Excluded-class audit

**FAIL** (partial — classes identified and counted, but **include/exclude decisions are PENDING** — not approved by the user)

The audit correctly identifies all 8 non-standard classes:

| Class | Count | Example file | Include/Exclude Decision |
|---|---|---|---|
| Block crack | 3 | China_Drone/train/…/China_Drone_001680.xml | **PENDING** |
| D01 | 179 | India/train/…/India_005014.xml | **PENDING** |
| D0w0 | 1 | India/train/…/India_006389.xml | **PENDING** |
| D11 | 45 | India/train/…/India_005835.xml | **PENDING** |
| D43 | 793 | Japan/train/…/Japan_004582.xml | **PENDING** |
| D44 | 5,057 | Japan/train/…/Japan_000096.xml | **PENDING** |
| D50 | 3,581 | Japan/train/…/Japan_004582.xml | **PENDING** |
| Repair | 1,046 | China_Drone/train/…/China_Drone_000556.xml | **PENDING** |

**Defect:** `data_audit.md` marks all decisions as `PENDING`. The user has not yet approved the include/exclude decision for any non-standard class.

---

## Check 5 — Splits (no image in two splits; leakage number in JSON)

**NOT RUN**

`ai-service/training/data/grouped_split.py` does not exist yet. `data/processed/splits/` does not exist. Leakage key is absent from `docs/data_audit.json`.

```
$ ls data/processed/
ls: data/processed/: No such file or directory
```

---

## Check 6 — YOLO conversion (label count, class IDs, normalised boxes, dropped log, data.yaml)

**NOT RUN**

`ai-service/training/data/convert_to_yolo.py` does not exist yet. `data/processed/yolo_rdd3/` and `data/processed/yolo_rdd4/` do not exist.

---

## Check 7 — rdd3 mapping (D00+D10 → crack_linear only)

**NOT RUN**

`convert_to_yolo.py` does not exist; cannot verify mapping.

---

## Check 8 — `docs/DATASETS.md` D40 wording and licence

**FAIL** (partial)

D40 wording **PASS**: Caveat 1 reads:
> "D40 is broader than "pothole" (rutting, bumps, separation, potholes). Mapping D40 -> pothole is an approximation and a stated limitation."

Licence **FAIL**: The licence field still reads:
> "Licence: read the licence field on the figshare page and record it here before any redistribution."

No actual licence name has been recorded in `DATASETS.md`.

---

## Check 9 — No dataset files tracked by git

**PASS**

```
$ git ls-files | grep -Ei "\.(jpg|png|pt|onnx)$"
EXIT:1  (empty — no matches)
```

No images, weights, or model files are tracked.

---

## Check 10 — Numbers in `data_audit.md` match `data_audit.json`

**PASS**

Cross-check script confirmed:

| Field | MD value | JSON value | Match |
|---|---|---|---|
| Total images with no annotations | 20,759 | 20,759 | ✓ |
| Total corrupted images | 0 | 0 | ✓ |
| All 13 split counts | (see Check 2) | identical | ✓ |

---

## Summary

| # | Check | Verdict |
|---|---|---|
| 1 | ruff + pytest | **FAIL** — 13 ruff errors in inventory.py; no data-script tests |
| 2 | inventory.py reproduces | **PASS** — second run identical |
| 3 | 20 XML spot-check | **PASS** — all consistent |
| 4 | Excluded-class audit | **FAIL** — decisions marked PENDING |
| 5 | Splits / leakage | **NOT RUN** — grouped_split.py missing |
| 6 | YOLO conversion | **NOT RUN** — convert_to_yolo.py missing |
| 7 | rdd3 mapping | **NOT RUN** — convert_to_yolo.py missing |
| 8 | DATASETS.md D40 + licence | **FAIL** — licence not filled in |
| 9 | No git-tracked dataset files | **PASS** |
| 10 | MD numbers match JSON | **PASS** |

## Overall Verdict: FAIL

### Defects to fix before re-verification

1. **ruff errors (13)** in `ai-service/training/data/inventory.py` — fix import order, trailing whitespace, unused `e`, long lines.
2. **No tests** for `inventory.py`, `grouped_split.py`, or `convert_to_yolo.py` (Check 1).
3. **Excluded-class decisions PENDING** — user must approve include/exclude for Block crack, D01, D0w0, D11, D43, D44, D50, Repair, then update `data_audit.md` (Check 4).
4. **`grouped_split.py` not implemented** — splits missing, leakage not measured (Check 5).
5. **`convert_to_yolo.py` not implemented** — YOLO output missing (Checks 6 & 7).
6. **`docs/DATASETS.md` licence field empty** — must record actual licence from figshare before re-verification (Check 8).
