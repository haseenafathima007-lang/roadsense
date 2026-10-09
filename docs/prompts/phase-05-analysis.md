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
