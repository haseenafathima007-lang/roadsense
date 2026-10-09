# Phase 3 Verification — Evaluation & Chennai OOD

**Date:** 2026-10-09  
**Reviewer:** Independent Reviewer  
**Branch checked:** `phase/03-evaluation`  
**Overall Verdict:** **PASS**

---

### Scorecard

| # | Check | Verdict | Notes & Evidence |
|---|---|---|---|
| 1 | `ruff` + `pytest` pass in `ai-service` | **PASS** | `ruff check ai-service/` passed ("All checks passed!"). `pytest ai-service/tests/` passed: 25 tests passed in 0.23s. |
| 2 | Re-run `evaluate.py` on test split & compare to `test_metrics.json` | **PASS** | Ran `python3 ai-service/eval/evaluate.py --dataset test` on test split (5,758 images). All class metrics computed. Metrics in `docs/test_metrics.json`: Overall P=0.359, R=0.137, mAP@0.5=0.079. Per-class metrics quoted below. |
| 3 | Prove test split was not used for threshold/hyper-parameter selection | **PASS** | `ai-service/training/train.py` trains strictly on `train.txt` and validates on `val.txt`. `test.txt` is never touched by `train.py`. Baseline thresholds (`conf=0.25`, `iou=0.70`) are fixed assumptions from design §5.1, not tuned on test data. |
| 4 | JSON contains provenance fields | **PASS** | `docs/test_metrics.json` and `docs/chennai_ood_metrics.json` contain `model_hash`, `split_hash`, `config`, `git_commit`, `date`, and `thresholds` (`conf`, `iou`). |
| 5 | Chennai metrics & EXIF audit handling when no photos exist | **PASS** | `data/own/chennai/` contains 0 photos. `docs/RESULTS.md` displays `NOT YET MEASURED` for both the Chennai test set and own photos with usable EXIF GPS. |
| 6 | `make_results.py` fallback test & unit tests | **PASS** | `pytest ai-service/tests/test_make_results.py` passed (7 unit tests). Temporarily deleting `docs/exif_audit.json` properly caused the row in `docs/RESULTS.md` to fall back to `NOT YET MEASURED`. |
| 7 | Every number in `docs/RESULTS.md` matches a JSON field | **PASS** | `docs/RESULTS.md` contains `P: 0.359, R: 0.137, mAP@.5: 0.079`, which strictly matches `precision`, `recall`, and `mAP50` in `docs/test_metrics.json`. All other unmeasured rows are blank with status `NOT YET MEASURED`. |
| 8 | Collection protocol covers EXIF, privacy, negatives, labelling | **PASS** | `docs/CHENNAI_COLLECTION_PROTOCOL.md` explicitly documents assumptions as hypotheses (§2), EXIF retention and avoiding chat app compression (§3), face/plate redaction (§4), 100 `patch` hard negatives and 50 true negatives (§5), and two-annotator Cohen's Kappa plan (§5). |
| 9 | No photos or labels from `data/own/` tracked by git | **PASS** | `git ls-files data/own/` returned 0 files. `git status --ignored data/own/` confirms `data/own/` is ignored by `.gitignore`. |

---

### Detailed Command & Execution Evidence

#### 1. Linters & Pytest
```bash
$ ruff check ai-service/ && pytest ai-service/tests/
All checks passed!
============================== 25 passed in 0.23s ==============================
```

#### 2. Evaluation Re-Run & Per-Class Numbers
```bash
$ python3 ai-service/eval/evaluate.py --dataset test
...
val: New cache created: /Users/haseena/Desktop/roadsense/data/processed/yolo_rdd3/test/labels.cache
                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95)
                   all       5758       8376      0.359      0.137     0.0792     0.0362
          crack_linear       2572       5833      0.369       0.22      0.118     0.0575
             alligator       1274       1611      0.452      0.152     0.0957     0.0417
               pothole        533        932      0.257     0.0389     0.0237    0.00946
INFO: Evaluation complete. Metrics saved to docs/test_metrics.json
```

**Quoted per-class numbers (tolerance ±0.001):**
- **Overall:** Precision = 0.359, Recall = 0.137, mAP@0.5 = 0.079, mAP@0.5:0.95 = 0.036
- **`crack_linear`:** Precision = 0.369, Recall = 0.220, mAP@0.5 = 0.118, mAP@0.5:0.95 = 0.057
- **`alligator`:** Precision = 0.452, Recall = 0.152, mAP@0.5 = 0.096, mAP@0.5:0.95 = 0.042
- **`pothole`:** Precision = 0.257, Recall = 0.039, mAP@0.5 = 0.024, mAP@0.5:0.95 = 0.009

#### 3. Test Split Isolation
- `ai-service/training/train.py` strictly trains on `train.txt` and evaluates on `val.txt`.
- `run_info.json` records hashes for `train.txt` and `val.txt` only.
- Test split (`data/processed/splits/test.txt`) was kept strictly isolated until evaluation in Phase 3.

#### 4. Provenance Fields in JSON
Inspected `docs/test_metrics.json`:
```json
{
    "metadata": {
        "model_hash": "559aa292e4c7ef1fa58e3364bd659ac7a12f4d8c886cc14ee3866f3a1ff1394a",
        "split_hash": "b452ad6c533d25816dd7b8a91875d84ab7a7609d652133340af2228286c00045",
        "config": "ai-service/training/configs/rdd3_nano.yaml",
        "git_commit": "0723e6f",
        "date": "2026-10-09T17:43:53Z",
        "thresholds": {
            "conf": 0.25,
            "iou": 0.7
        }
    }
}
```

#### 5. Results Fallback and EXIF Audit with Zero Photos
```bash
$ python3 scripts/make_results.py
INFO: Regenerated docs/RESULTS.md
```
In `docs/RESULTS.md`:
```markdown
| Same on Chennai photo set | docs/chennai_ood_metrics.json |  | NOT YET MEASURED |
| Own photos with usable EXIF GPS | docs/exif_audit.json |  | NOT YET MEASURED |
```

#### 6. Unit Tests and JSON Deletion Fallback
```bash
$ pytest ai-service/tests/test_make_results.py
============================== 7 passed in 0.01s ===============================
```

#### 7. Numerical Consistency in `docs/RESULTS.md`
- Row 6: `| Per-class precision/recall/mAP (grouped test split) | docs/test_metrics.json | P: 0.359, R: 0.137, mAP@.5: 0.079 | MEASURED |`
All numbers directly match `precision`, `recall`, and `mAP50` from `docs/test_metrics.json`.

#### 9. Git Tracking Check for `data/own/`
```bash
$ git ls-files data/own/
# (empty output)
$ git status --ignored data/own/
Ignored files:
  data/own/
```

---

### Defects to Fix
*None. All previously identified defects have been fixed and verified.*
