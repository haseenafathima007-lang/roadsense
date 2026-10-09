# AGENTS.md: rules for every AI agent working in this repo

## Read order (every session)
1. `docs/03_image_only_java_redesign.md` (the design; source of truth)
2. `docs/ARCHITECTURE.md`, `docs/CONTRACTS.md`, `docs/ROADMAP.md`
3. The current phase file in `docs/prompts/`
4. `docs/PHASE_STATUS.md`

## Principles
- Python is the eyes, Java is the brain. Python = one stateless detection service. All decisions live in Java.
- Input is images only. No video, tracker, GPX/trace, or time interpolation. Do not reintroduce them.
- When in doubt about merging two photos into one defect: create NEEDS_REVIEW, never auto-merge.
- No claims of physical depth or true size. Severity is area-ratio on the best view.

## Status discipline (non-negotiable)
- Never invent a metric, threshold validation, or benchmark. Unmeasured = `NOT YET MEASURED`.
- Numbers in docs come only from JSON/CSV files written by scripts in `scripts/`, `ai-service/eval/` or `mvn verify`.
- Thresholds in `thresholds.yaml` are starting assumptions; keep the `UNVALIDATED` comment until measured.
- Library names in the design are candidates. Before adding any dependency: check the current version, licence
  and maintenance, then record it in `docs/DEPENDENCIES.md`. Flag copyleft licences (e.g. AGPL) explicitly.
- Synthetic or staged data must be labelled as such everywhere it appears (UI banner, docs, filenames).

## Java rules (`java-app/`)
- Java 21, Maven, JUnit 5 + AssertJ. `mvn verify` must pass before any commit that touches Java.
- Package map is in design §4.1. `ui` contains no domain logic. `domain` has no I/O imports.
- Value objects are `record`s. State changes go through guarded methods. Illegal transitions throw `IllegalTransitionException`.
- Every pattern must serve a listed extension point (design §4.2). No pattern for its own sake.
- Inject interfaces (`DamageDetector`, `ExifReader`, ...). Tests use `FakeDetector`; no test may need the Python service or network.

## Python rules (`ai-service/`)
- Python 3.11, FastAPI, `ruff` + `pytest` clean. Service is stateless and contains no business logic.
- Weights, datasets, and run folders are never committed (use `.gitignore`; weights via GitHub Releases or Git LFS).

## Workflow rules
- Plan first: produce an Implementation Plan, wait for approval, then implement.
- Work only on branch `phase/NN-name`. Never push to `main`. Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).
- Small commits. Run the tests before every commit.
- Ask before: large downloads (> 500 MB), network calls other than documented ones, deleting files, changing design decisions.
- STOP and ask the user when a required value is missing (e.g. severity thresholds from the main document §5).
- Never commit secrets, API keys, real photos of people/plates, or files under `data/`.

## Privacy
Read EXIF first, then store images without EXIF. Blur faces and plates before any image or crop is stored or exported; blur more rather than less.

## Definition of done (every phase)
Code + tests + docs updated + `mvn verify` / `pytest` / `ruff` green + `docs/PHASE_STATUS.md` ticked + no unlabelled numbers.
