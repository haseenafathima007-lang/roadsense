# Phase 0: Repo standards
**Branch:** `phase/00-repo-standards` | **Builder:** Gemini 3.8 Flash | **Verifier:** Gemini 3.1 Pro | **Quota:** light | **Depends on:** scaffold committed

## Goal
Working Java + Python skeletons with green CI, so every later phase starts from a clean, checked base.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md, docs/ARCHITECTURE.md, docs/ROADMAP.md. This project is image-only,
Java-centred (Python = stateless YOLO endpoint, Java = decision engine). You are doing PHASE 0 only: repo standards. Implement NO features.

STEP 1 (plan first): produce an Implementation Plan listing every file you will create/change. Wait for my approval.

STEP 2 (after approval):
a) java-app/: Maven project, groupId com.roadai, artifactId road-ai-app, Java 21. Create one package per entry in design section 4.1
   (domain, perception, imaging, geo, analysis, priority, lifecycle, persistence, service, security, config, ui) with a package-info.java
   that states the package's responsibility in one sentence. Add JUnit 5, AssertJ, JaCoCo (report on verify), Spotless with
   google-java-format (check on verify). Add ONE smoke test. Look up the current stable plugin/dependency versions yourself
   (do not rely on memory) and record name, version, licence, date checked in docs/DEPENDENCIES.md.
b) ai-service/: pyproject.toml (project metadata, optional-dependencies [dev] with ruff and pytest), app/__init__.py,
   tests/test_smoke.py, ruff config. No model or FastAPI code yet.
c) Make .github/workflows/ci.yml work: run exactly its commands locally and show the output.
d) docs/uml/README.md explaining the diagram convention (Mermaid or PlantUML; pick one, record it as docs/adr/0003-uml-tool.md).
e) Update docs/PHASE_STATUS.md only after I confirm.
f) Do not delete docs/03_PLACE_DESIGN_DOC_HERE.md unless docs/03_image_only_java_redesign.md exists; if it is missing, tell me.

RULES: no network downloads other than Maven/pip dependencies; no secrets; conventional commits on this branch; never touch main.

OUTPUT: (1) list of files created, (2) every command you ran with its real output, (3) anything you could not do and why.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 0 of this repo. Follow the common verifier rules in docs/prompts/00_START_HERE.md
(read-only on source; write only docs/verification/phase-00.md; PASS/FAIL/NOT RUN with quoted evidence).
Checks:
1. docs/03_image_only_java_redesign.md exists. 
2. `cd java-app && mvn -B verify` succeeds. Quote the final BUILD line and test count.
3. java-app has all 12 packages from design section 4.1, each with package-info.java.
4. JaCoCo report generated (path) and Spotless check runs as part of verify.
5. `cd ai-service && pip install -e ".[dev]" && ruff check . && pytest -q` succeed.
6. .github/workflows/ci.yml commands match what you just ran; YAML parses.
7. docs/DEPENDENCIES.md rows exist for every dependency added, with licence and date filled. List any missing.
8. No video/tracker/GPX wording introduced anywhere (grep -ri "bytetrack|gpx|video" excluding docs/03_* and ADR 0001).
9. .gitignore prevents committing data/, *.pt, *.onnx, runs/ (prove with `git check-ignore -v`).
10. No secrets or large files committed (`git ls-files | xargs du -ch | tail -1`, and a grep for key-like strings).
Finish with a one-paragraph verdict and the list of defects.
````

## EXPECTED OUTPUTS
- `java-app/pom.xml`, 12 packages with `package-info.java`, `SmokeTest`, `target/site/jacoco/` after verify.
- `ai-service/pyproject.toml`, `tests/test_smoke.py`.
- `docs/DEPENDENCIES.md` rows filled; `docs/adr/0003-uml-tool.md`.
- Commands: `mvn -B verify` -> `BUILD SUCCESS`, 1 test; `pytest -q` -> `1 passed`; `ruff check .` -> `All checks passed!`.
- `docs/verification/phase-00.md` with 10 PASS rows.

## Commit / PR
`chore: add java and python skeletons, CI and dependency register` -> PR -> merge -> tick Phase 0.
