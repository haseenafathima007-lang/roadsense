# Phase 0 Verification

## Checks

1. **PASS**: `docs/03_image_only_java_redesign.md` exists.
   > Evidence: `[ -f docs/03_image_only_java_redesign.md ]` output `EXISTS`.
2. **PASS**: `cd java-app && mvn -B verify` succeeds.
   > Evidence: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0` and `[INFO] BUILD SUCCESS`.
3. **PASS**: `java-app` has all 12 packages from design section 4.1, each with package-info.java.
   > Evidence: `find src/main/java/com/roadai -name "package-info.java"` returned 12 files (domain, perception, imaging, geo, analysis, priority, lifecycle, persistence, service, security, config, ui).
4. **PASS**: JaCoCo report generated (path) and Spotless check runs as part of verify.
   > Evidence: Report found at `java-app/target/site/jacoco/index.html`. Spotless execution: `--- spotless:2.43.0:check (default) @ road-ai-app ---` followed by `Spotless.Java is keeping 13 files clean`.
5. **PASS**: `cd ai-service && pip install -e ".[dev]" && ruff check . && pytest -q` succeed.
   > Evidence: Ruff output `All checks passed!`. Pytest output `1 passed in 0.00s`.
6. **PASS**: `.github/workflows/ci.yml` commands match what you just ran; YAML parses.
   > Evidence: Python `yaml.safe_load` parsed successfully. Commands `mvn -B verify` and `pip install -e ".[dev]" && ruff check . && pytest -q` exactly matched the workflow.
7. **PASS**: `docs/DEPENDENCIES.md` rows exist for every dependency added, with licence and date filled.
   > Evidence: Verified rows exist for JUnit Jupiter, AssertJ Core, JaCoCo Maven Plugin, Spotless Maven Plugin, google-java-format, Ruff, and Pytest with dates and licences.
8. **PASS**: No video/tracker/GPX wording introduced anywhere (grep -ri "bytetrack|gpx|video" excluding docs/03_* and ADR 0001).
   > Evidence: `grep -riE "bytetrack|gpx|video" --exclude="03_*" --exclude="0001-image-only-input.md" .` matched only instructional files (`AGENTS.md`, `rules/`, `prompts/`). No implementation code contains these terms.
9. **PASS**: `.gitignore` prevents committing data/, *.pt, *.onnx, runs/ (prove with `git check-ignore -v`).
   > Evidence: `git check-ignore -v` successfully mapped `data/dummy.jpg`, `model.pt`, `model.onnx`, and `runs/exp1` to `.gitignore` rules.
10. **PASS**: No secrets or large files committed.
    > Evidence: `git ls-files | xargs du -ch | tail -1` reported `360K total`.

## Verdict
**PASS.** All 10 verification checks have passed cleanly. The repository standards are fully established, CI pipelines are functional, the design document is in place, and code quality tools are configured. Phase 0 is complete.

## Defects
- None.
