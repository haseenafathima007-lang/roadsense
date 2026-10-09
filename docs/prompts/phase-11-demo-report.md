# Phase 11: Demo, UML, OOP document, report
**Branch:** `phase/11-demo-report` | **Builder:** Gemini 3.1 Pro (writing) + Gemini 3.8 Flash (UML/boilerplate) | **Verifier:** Claude Sonnet 5.5 | **Quota:** medium | **Depends on:** Phase 10

## Goal
A reproducible demo with honest labels, complete UML and OOP mapping, and a report that quotes only measured numbers.

## MASTER PROMPT
````text
CONTEXT: Read AGENTS.md, docs/03_image_only_java_redesign.md (4.2, 10, 11, 12, 13, 14), docs/RESULTS.md. PHASE 11: demo + documentation. Do not add features.
STEP 1: Implementation Plan, wait for approval.
STEP 2:
 1. ai-service/scripts/make_demo_data.py: takes real RDD2022 TEST-split images (the labelled grouped split) and assigns SYNTHETIC coordinates along a made-up route in Chennai and synthetic report dates; creates before/after pairs ONLY from user-provided pairs, otherwise
    clearly labelled staged pairs. Output to sample-data/demo/ (check the dataset licence before committing any image; if unsure keep images out of git and commit only the generator + a manifest). Manifest flags every item SYNTHETIC_LOCATION / STAGED. The app's banner must trigger from this flag.
 2. docs/DEMO_PLAYBOOK.md final: live mode (own Chennai photos with EXIF through the real model; quote only RESULTS.md numbers) and offline mode (banner always on), with the story: several reporters -> duplicates merge -> ranked queue -> after photo passes proof-of-visit -> repaired defect reappears and priority rises. Step-by-step click script with expected screen states.
 3. docs/uml/: class diagrams per package, sequence diagram of ReportPipeline, state diagrams (from Phase 9), component diagram (Python service, Java engine, SQLite, OSM cache). Diagrams generated from the real code structure (verify they match class names).
 4. docs/OOP_DESIGN.md: complete the design 4.2 table with real class names and file paths, and add two short code excerpts per major pattern. Include a section "Rubric mapping" with placeholders for the supervisor's marking scheme.
 5. docs/REPORT.md (or the format I specify): sections: problem, design decisions (image-only ADR), architecture, data and its caveats (D40, labelling, spot bias, EXIF), methods, results (ONLY from RESULTS.md; NOT YET MEASURED rows stay as such), limitations (design section 12 + what you found), ethics/privacy, future work.
 6. README.md final: accurate status, quickstart that works from a clean clone (run it), screenshots, licence section (confirm with me which licence), dataset and OSM attribution.
 7. scripts/honesty_check.py: scans docs/ for numbers near metric words (mAP, precision, recall, kappa, %, ms, tests) that do not appear in any docs/*.json; exits non-zero on findings. Add it to CI.
 8. Tag v0.1.0 instructions in CHANGELOG (do not push tags yourself).
OUTPUT: files, commands + output (including a clean-clone run), honesty_check output.
````

## VERIFICATION PROMPT
````text
You are an independent reviewer for PHASE 11. Follow docs/prompts/00_START_HERE.md. Write docs/verification/phase-11.md.
Checks:
1. Clean clone test: clone into a temp folder, follow README quickstart exactly, report which steps fail.
2. `mvn -B verify`, `pytest -q`, `ruff check .`, `python scripts/honesty_check.py` all succeed.
3. Run make_demo_data.py; manifest flags every item; banner triggers in the app data flag path (code trace).
4. docs/REPORT.md: every number maps to a JSON (list the mapping); NOT YET MEASURED rows preserved; limitations section includes EXIF issues, spot bias, D40 caveat, single-frame false positives, staged pairs.
5. docs/OOP_DESIGN.md: each class/path in the table exists (script check); each pattern has an excerpt that matches the real code.
6. UML class names exist in code (script check or list mismatches).
7. DEMO_PLAYBOOK steps are executable (follow 5 steps and report the screen states you can verify statically; mark UI-runtime items NOT RUN).
8. Licence and attribution: dataset (RDD2022), OSM ODbL, third-party libs. List gaps. Confirm no dataset images are committed unless their licence allows it.
9. No video/tracker wording outside ADR 0001 and the design document.
10. CHANGELOG updated; no stray large files.
Verdict + defects.
````

## EXPECTED OUTPUTS
- `make_demo_data.py` + manifest, `DEMO_PLAYBOOK.md`, UML set, `OOP_DESIGN.md`, `REPORT.md`, final `README.md`, `scripts/honesty_check.py` wired into CI.
- Pass criteria: clean-clone quickstart works; honesty check clean; every report number traceable; synthetic/staged data labelled.

## Commit / PR
`docs: demo, uml, oop design, report and honesty check` -> tag `v0.1.0` yourself after merge.
