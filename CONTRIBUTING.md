# Contributing

- Branches: `phase/NN-name` for phase work, `fix/...`, `docs/...`. Never commit to `main`.
- Commits: Conventional Commits. One logical change each.
- PRs: use the template; link the phase file; attach `docs/verification/phase-NN.md`.
- Code style: Java via Spotless (google-java-format); Python via ruff.
- Tests: new behaviour needs tests. Java tests must not need the network or the Python service.
- Docs: update `docs/PHASE_STATUS.md`, `docs/DEPENDENCIES.md`, and ADRs for design changes.
- Results: never type a metric by hand. Run the script that writes the JSON, then `scripts/make_results.py`.
