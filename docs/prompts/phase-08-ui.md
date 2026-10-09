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
