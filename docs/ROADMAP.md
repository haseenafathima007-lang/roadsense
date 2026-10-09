# Roadmap (design §6). Prompts live in `docs/prompts/`.

| Phase | Goal | Language | Prompt |
|---|---|---|---|
| 0 | Repo standards | both | phase-00-repo-standards.md |
| 1 | Data hardening | Python | phase-01-data-hardening.md |
| 2 | Training | Python | phase-02-training.md |
| 3 | Evaluation + Chennai OOD | Python | phase-03-evaluation-chennai.md |
| 4 | Perception + inputs | Python + Java | phase-04-perception-inputs.md |
| 5 | Analysis (dedup, severity) | Java | phase-05-analysis.md |
| 6 | Priority + OSM | Java | phase-06-priority-osm.md |
| 7 | Persistence + roles + pipeline | Java | phase-07-persistence-roles.md |
| 8 | UI (+ survey mode, second priority) | Java | phase-08-ui.md |
| 9 | Verification (proof-of-visit) | Java | phase-09-verification.md |
| 10 | Hardening | both | phase-10-hardening.md |
| 11 | Demo + report | Java + docs | phase-11-demo-report.md |

Decision gate (after phases 4-9 pass): pure-Java ONNX inference (design §8). Otherwise keep RemoteYoloDetector.
Phases 1-3 (Python/data) and 4-5 (Java with FakeDetector) can run in parallel if you have two working branches.

### Phase 5 notes
- Introduce union-area damage index calculation in Java domain logic.
