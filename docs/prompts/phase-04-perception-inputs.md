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
