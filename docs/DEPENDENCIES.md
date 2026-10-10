# Dependencies (candidates until verified)
Agents: before adding a dependency, check current version, licence, maintenance, then fill a row.

| Name | Used for | Version | Licence | Verified on (date) | Notes |
|---|---|---|---|---|---|
| Ultralytics YOLO (candidate) | detector training/inference | | AGPL-3.0 (verify) | | Copyleft: decide consciously before distributing the app; alternatives exist |
| FastAPI | Python service | | | | |
| metadata-extractor (candidate) | EXIF reading | | | | |
| OpenCV Java (candidate) | ORB similarity | | | | Fallback: perceptual hash behind ImageSimilarity |
| ONNX Runtime Java (stretch) | pure-Java inference | | | | Verify Apple Silicon |
| SQLite JDBC | persistence | | | | |
| JavaFX | UI | | | | |
| OSM data (Overpass/Geofabrik) | road links | n/a | ODbL | | Attribution required; respect Overpass usage policy |
| JUnit Jupiter | Java unit/integration testing | 5.11.4 | EPL-2.0 | 2026-10-09 | Maintained, standard Java 21 test engine |
| AssertJ Core | Fluent assertions for Java tests | 3.27.3 | Apache-2.0 | 2026-10-09 | Permissive licence, pairs with JUnit 5 |
| JaCoCo Maven Plugin | Java code coverage reporting | 0.8.12 | EPL-2.0 | 2026-10-09 | Compatible with Java 21, runs on verify |
| Spotless Maven Plugin | Code formatting enforcement | 2.43.0 | Apache-2.0 | 2026-10-09 | Configured with google-java-format, runs on verify |
| google-java-format | Java source code formatter | 1.24.0 | Apache-2.0 | 2026-10-09 | Google Java Style formatting engine used by Spotless |
| Ruff | Python linter and formatter | >=0.8.0 | MIT / Apache-2.0 | 2026-10-09 | Fast Rust-based linter, configured in pyproject.toml |
| Pytest | Python test framework | >=8.0.0 | MIT | 2026-10-09 | Standard test runner, configured in pyproject.toml |
| Pillow | Image dimensions and validation | >=10.0.0 | HPND | 2026-10-09 | Used in data hardening |
| ImageHash | Perceptual hashing for dataset split grouping | >=4.3.0 | BSD 2-Clause | 2026-10-09 | Prevents sequence leakage |
| tqdm | Progress bar for data scripts | >=4.66.0 | MIT/MPLv2 | 2026-10-09 | Dev tool |
| PyYAML | YAML serialisation for data.yaml in convert_to_yolo.py | 6.0.3 | MIT | 2026-10-09 | Stdlib-compatible |
| FastAPI | Python detection service | 0.143.0 | MIT | 2026-10-10 | |
| Starlette | ASGI framework (FastAPI dep) | 1.7.0 | BSD 3-Clause | 2026-10-10 | |
| Uvicorn | ASGI server | 0.54.0 | BSD 3-Clause | 2026-10-10 | |
| python-multipart | Multipart form parsing | 0.0.32 | Apache-2.0 | 2026-10-10 | |
| httpx | Async HTTP client (tests) | 0.28.1 | BSD 3-Clause | 2026-10-10 | |
| metadata-extractor | EXIF reading in Java | 2.19.0 | Apache-2.0 | 2026-10-10 | |
| snakeyaml | YAML config loading in Java | 2.4 | Apache-2.0 | 2026-10-10 | |
