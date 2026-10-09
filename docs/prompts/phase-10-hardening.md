# Phase 10: Hardening (privacy, agreement metrics, stability, ONNX gate)
**Branch:** `phase/10-hardening` | **Builder:** Claude Opus 5.5 for the security/privacy review, Claude Sonnet 5.5 for fixes | **Verifier:** Gemini 3.1 Pro | **Quota:** heavy | **Depends on:** Phases 1-9

## Goal
Close the honesty gaps: privacy actually works, agreement and stability numbers exist with provenance, and the ONNX decision gate is resolved on evidence.

## You do (manual)
Collect expert severity ratings (rating sheet CSV), labelled same/different defect photo pairs, and a few real before/after pairs if available. The agent builds tools and analysis scripts; it must not invent any of this data.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (5.10, 8, 12, 13), docs/RESULTS.md. PHASE 10: hardening. Both languages.
STEP 1: Implementation Plan, wait for approval. Start with a written security + privacy review of the current code (threat list, findings table, severity), then fix.
STEP 2:
 1. Privacy: ExifStripper (read EXIF first, store stripped), Redactor interface + BlurRedactor for faces and plates in stored images and crops (candidate detectors: verify licences; default to blurring MORE not less);
    export paths also redact; retention + delete options work end to end (images, crops, DB rows). Tests on generated images with synthetic "plate-like" and "face-like" regions as far as feasible; state clearly what is NOT covered
    and add a manual redaction check to docs/PRIVACY_CHECKLIST.md. Do not claim a detection rate until measured on real samples; write NOT YET MEASURED.
 2. Upload security: size/type validation in Java too, no path traversal from file names, temp-file cleanup, request timeouts, API key never logged. Add tests.
 3. scripts/kappa.py: Cohen's kappa (and weighted kappa) between model severity and expert ratings from a CSV rating sheet; writes docs/severity_kappa.json with n, kappa, confusion table. Provide a CSV template and unit tests with a hand-computed example.
 4. Quality-gate agreement tool: compares gate flags with human judgement CSV; writes docs/quality_gate_agreement.json.
 5. Dedup evaluation tool: reads labelled same/different pairs CSV, runs the assembler's similarity + distance rules, writes docs/dedup_eval.json (precision, recall, confusion). Sweep thresholds on a TUNING subset and report on a held-out subset; never report tuned-on numbers as final.
 6. Proof-of-visit evaluation tool on real pairs (if present): writes docs/proof_of_visit_eval.json.
 7. Benchmark: upload-to-ranked-ticket time over N generated images (stopwatch in code, hardware recorded) -> docs/benchmark.json.
 8. Coverage: record mvn verify test count + JaCoCo line coverage into docs/test_summary.json via script.
 9. Extend scripts/make_results.py to map every JSON above into RESULTS.md; missing -> NOT YET MEASURED. Re-run priority stability from Phase 6.
 10. ONNX decision gate (design 8): ONLY run if Phases 4-9 are verified. Implement OnnxDetector behind DamageDetector (letterboxing, decoding, NMS in Java), run the parity set against RemoteYoloDetector, write docs/onnx_parity.json
     (box/class/confidence deltas, speed, platform). Gate result = pass/fail by criteria you propose and I approve beforehand. If it fails or I decline, keep RemoteYoloDetector and delete nothing else.
 11. Dependency licence audit: fill docs/DEPENDENCIES.md; flag AGPL/GPL implications for distribution.
RULES: no fabricated data, no tuning on the final test, every number from a JSON.
OUTPUT: review findings table, files, commands + output, JSON paths, gate decision.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 10. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-10.md.
Checks:
1. `mvn -B verify` and `pytest -q` + `ruff check .` green. Quote counts.
2. Privacy: stored images contain no EXIF (inspect a stored file with an EXIF tool or code); redaction is applied on storage AND export paths (trace both). Retention/delete removes files and DB rows (test evidence).
3. Upload security tests: oversize, wrong type, `../` file names, zero-byte, corrupt JPEG. API key absent from logs (run and grep).
4. kappa.py: verify the unit test with a hand-computed example and recompute kappa independently on the sample CSV (show the arithmetic).
5. Dedup tool: confirm the tuning/held-out split is separate and the reported numbers are from held-out data.
6. Every JSON referenced by RESULTS.md exists and the numbers match; list any number without a source. Missing data must read NOT YET MEASURED (not 0, not blank).
7. Benchmark JSON includes hardware, N, and method; numbers reproducible within a stated tolerance (re-run).
8. ONNX gate: either docs/onnx_parity.json exists with the approved criteria and a clear pass/fail, or the report shows the gate was skipped by decision. No half state.
9. docs/DEPENDENCIES.md has a licence for each dependency; AGPL/GPL implications are called out.
10. Honesty sweep: grep the repo for unlabelled synthetic/staged data and for percentages/mAP/kappa values in docs not produced by scripts.
Verdict + defects.
````

## EXPECTED OUTPUTS
- `docs/severity_kappa.json`, `quality_gate_agreement.json`, `dedup_eval.json`, `proof_of_visit_eval.json`, `benchmark.json`, `test_summary.json`, `priority_stability.json`, optionally `onnx_parity.json`; `docs/PRIVACY_CHECKLIST.md`; updated `RESULTS.md`.
- Pass criteria: every row in RESULTS.md has a script-written source or says NOT YET MEASURED; privacy verified on storage and export; gate decided on evidence.

## Commit / PR
`feat: privacy, evaluation tools, benchmarks and onnx decision gate`
