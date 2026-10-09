# Datasets

## Primary: RDD2022
- Source: https://doi.org/10.6084/m9.figshare.21431547 (figshare)
- Code/info: https://github.com/sekilab/RoadDamageDetector (also hosts earlier RDD2018/2019/2020 and links)
- Paper to cite: Arya et al., "RDD2022: A multi-national image dataset for automatic road damage detection" (arXiv 2209.08538) and the CRDDC2022 challenge papers. Verify the exact citation on the figshare page.
- Content (verify on download; Phase 1 audit is the source of truth): ~47k images from Japan, India, Czech Republic, Norway, United States, China (drone, motorbike); PASCAL VOC XML annotations.
- Classes in the challenge: D00 longitudinal crack, D10 transverse crack, D20 alligator crack, D40 other damage including potholes.
- Known caveats to audit, not assume:
  1. D40 is broader than "pothole" (rutting, bumps, separation, potholes). Mapping D40 -> pothole is an approximation and a stated limitation.
  2. D00/D10 labelling is not consistent across countries -> design uses a 3-class scheme + Java orientation (design D10 mitigation).
  3. Extra classes (e.g. D01, D11, D43, D44) may appear in some XML files -> label audit, explicit include/exclude decision.
  4. Public test splits are released without labels -> we build our own labelled split from the labelled data.
  5. No explicit sequence IDs -> grouped split must be approximated and leakage measured.
- Licence: CC BY 4.0 (Creative Commons Attribution 4.0 International), as stated on the figshare page. Attribution: Arya et al. (2022). Do not redistribute without this attribution.
- Excluded classes (all EXCLUDE — ignore annotations, images become background negatives if they contain only these):
  - D43 (793 annotations, Japan only) — unclear semantics, not in challenge spec
  - D44 (5,057 annotations, Japan + India) — unclear semantics, not in challenge spec
  - D50 (3,581 annotations, Japan + India) — unclear semantics, not in challenge spec
  - D01 (179 annotations, India) — variant of D00, orientation ambiguous; covered by D00 already
  - D11 (45 annotations, India) — variant of D10; covered by D10 already
  - Block crack (3 annotations, China_Drone) — too few examples; not in challenge spec
  - D0w0 (1 annotation, India) — single annotation, likely labelling error
  - Repair (1,046 annotations, China) — candidate for future "patch" hard-negative class (see docs/PATCH_CLASS_PLAN.md); excluded now, no data invented

## Own data: Chennai photo set (out-of-distribution test)
Photos with EXIF kept, private, protocol in `docs/CHENNAI_COLLECTION_PROTOCOL.md` (created in Phase 3).

## Local layout
`data/raw/RDD2022/` original download, `data/processed/yolo/` converted dataset, `data/own/chennai/` private photos. All git-ignored.
