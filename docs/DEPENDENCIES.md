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
