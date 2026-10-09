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
