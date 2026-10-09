# Datasets

## Primary: RDD2022
- Source: https://doi.org/10.6084/m9.figshare.21431547 (figshare)
- Code/info: https://github.com/sekilab/RoadDamageDetector (also hosts earlier RDD2018/2019/2020 and links)
- Paper to cite: Arya et al., "RDD2022: A multi-national image dataset for automatic road damage detection" (arXiv 2209.08538) and the CRDDC2022 challenge papers. Verify the exact citation on the figshare page.
- Content (verify on download; Phase 1 audit is the source of truth): ~47k images from Japan, India, Czech Republic, Norway, United States, China (drone, motorbike); PASCAL VOC XML annotations.
- Classes in the challenge: D00 longitudinal crack, D10 transverse crack, D20 alligator crack, D40 other damage including potholes.
- Known caveats to audit, not assume:
  1. D40 is broader than "pothole" (rutting, bumps, separation, potholes). Mapping D40 -> pothole is an approximation; record it as a limitation.
  2. D00/D10 labelling is not consistent across countries -> design uses a 3-class scheme + Java orientation (design D10 mitigation).
  3. Extra classes (e.g. D01, D11, D43, D44) may appear in some XML files -> label audit, explicit include/exclude decision.
  4. Public test splits are released without labels -> we build our own labelled split from the labelled data.
  5. No explicit sequence IDs -> grouped split must be approximated and leakage measured.
- Licence: read the licence field on the figshare page and record it here before any redistribution. Do not commit images except tiny approved fixtures.

## Own data: Chennai photo set (out-of-distribution test)
Photos with EXIF kept, private, protocol in `docs/CHENNAI_COLLECTION_PROTOCOL.md` (created in Phase 3).

## Local layout
`data/raw/RDD2022/` original download, `data/processed/yolo/` converted dataset, `data/own/chennai/` private photos. All git-ignored.
