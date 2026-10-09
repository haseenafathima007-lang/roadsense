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
