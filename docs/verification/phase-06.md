# Phase 6 Verification Report (Priority + OSM)

**Reviewer:** Independent AI Agent  
**Date:** 2026-10-10  
**Status:** PASS

## 1. Build and Coverage
- **`mvn -B verify`**: Green (86 tests passed, 0 failures, 0 errors).
- **Coverage**: Priority and Geo packages are fully covered by `PriorityCalculatorTest`, `PriorityStabilityTest`, `RoadLinkSnapperTest`, and `CachedOsmContextTest`. Test count has increased appropriately.

## 2. PriorityResult Examples Check
Recomputing two examples from `PriorityCalculatorTest`:
1. **HIGH severity, 2 obs, no road snap**: 
   - Severity = HIGH -> `3.0`
   - Road = No snap -> `1.0` (neutral)
   - Exposure = Default -> `1.0`
   - Recurrence = 2 obs -> `1.0 + (2-1)*0.2 = 1.2`
   - **Result**: `3.0 * 1.0 * 1.0 * 1.2 = 3.6`. Code expects and computes `3.6`.
2. **CorroborationFactor**: 2 distinct reporters (hashes)
   - **Result**: `1.0 + (2-1)*0.2 = 1.2`. Code expects and computes `1.2`.

## 3. Add-a-Factor Test
- The test `newFactorDoesNotEditExistingClasses` in `PriorityCalculatorTest` implements a new `tag_boost` anonymous inner class implementing `PriorityFactor`.
- It demonstrates that passing it into the `PriorityCalculator` constructor correctly multiplies the score (by `1.5`) without requiring any modification to existing framework classes, satisfying the open/closed requirement.

## 4. Snapper Behaviours
- **Crossroads/Parallel Roads**: `RoadLinkSnapperTest.snapsToNearestSegmentCrossroads` and `snapsToNearestSegmentParallelRoads` verify correct closest-segment snapping.
- **Far Point**: `farPointExceedsMaxDistance` proves that a point exceeding the max snap distance (configured in thresholds) gracefully returns `Optional.empty` (an explicit "no link" value), rather than blindly snapping or throwing an error.

## 5. Network Isolation in `CachedOsmContext`
- A `grep_search` across `java-app/src/main/java/com/roadai/geo` for `java.net` and `HttpClient` returned zero hits.
- The OSM file is strictly read from local InputStream at startup. Tests run without internet access successfully.

## 6. Fetch Script Compliance
- `scripts/fetch_osm.py` explicitly captures the ODbL license, download date, bbox, and Overpass query into `data/osm/README.md`.
- `data/osm/*.geojson` and `*.json` were successfully added to `.gitignore`.
- UI/README attribution text (`Data © OpenStreetMap contributors, ODbL 1.0 https://osm.org/copyright`) is enforced by the script.

## 7. Stability Tests
- `PriorityStabilityTest` deterministic seed check: Ran `mvn test -Dtest=PriorityStabilityTest` twice; it correctly overwrites `docs/priority_stability.json` with identical synthetic results (`stability_rate_pct = 100.00`).
- The JSON output is entirely script-generated.
- `RESULTS.md` correctly still states `NOT YET MEASURED` for "Priority top-N stability" since the result has not been mapped yet by `scripts/make_results.py`.

## 8. Spot Mode Condition Rating
- A review of `LinkSummaryBuilder.java` reveals it solely returns a map of grouped `Defect` objects per `RoadLink` via `LinkSummary`. No PCI or aggregated condition ratings are spuriously calculated.

## 9. Unlocated Defects Exclusion
- `PriorityCalculatorTest.noLocationExcludedFromRankedMap` explicitly mocks a defect returning a null `LocationFix` and asserts that `rankForMap` securely filters it out of the output queue.
- `PriorityCalculatorTest.locationNeededListContainsNullLocation` proves they are shunted correctly to the unlocated queue.

## 10. UNVALIDATED Comments Guard
- `UnvalidatedSeverityGuardTest` checks that all thresholds pass the `UNVALIDATED` check.
- `grep_search` over `thresholds.yaml` confirms that all new `road_class_weight`, `recurrence_step`, `corroboration_step`, `exposure_default`, `snap_max_distance_m`, and `top_n_stability` fields prominently carry the `# UNVALIDATED starting assumption` comment inline.

## Verdict
**PASS**. No defects found. All strict Phase 6 requirements from `docs/prompts/00_START_HERE.md` and `AGENTS.md` have been met. Code architecture remains clean and strictly adherent to the design document.
