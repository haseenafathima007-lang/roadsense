# Data Audit Report

## Images per Split
- **China_Drone/train**: 2401
- **China_MotorBike/test**: 500
- **China_MotorBike/train**: 1977
- **Czech/test**: 709
- **Czech/train**: 2829
- **India/test**: 1959
- **India/train**: 7706
- **Japan/test**: 2627
- **Japan/train**: 10506
- **Norway/test**: 2040
- **Norway/train**: 8161
- **United_States/test**: 1200
- **United_States/train**: 4805

## Annotations per Class per Country
### China_Drone
- Block crack: 3
- D00: 1426
- D10: 1263
- D20: 293
- D40: 86
- Repair: 769
### China_MotorBike
- D00: 2678
- D10: 1096
- D20: 641
- D40: 235
- Repair: 277
### Czech
- D00: 988
- D10: 399
- D20: 161
- D40: 197
### India
- D00: 1555
- D01: 179
- D0w0: 1
- D10: 68
- D11: 45
- D20: 2021
- D40: 3187
- D43: 57
- D44: 1062
- D50: 28
### Japan
- D00: 4049
- D10: 3979
- D20: 6199
- D40: 2243
- D43: 736
- D44: 3995
- D50: 3553
### Norway
- D00: 8570
- D10: 1730
- D20: 468
- D40: 461
### United_States
- D00: 6750
- D10: 3295
- D20: 834
- D40: 135

## Excluded-Class Label Audit
| Class | Count | Examples | Include/Exclude Decision |
|---|---|---|---|
| Block crack | 3 | China_Drone/train/annotations/xmls/China_Drone_001680.xml<br>China_Drone/train/annotations/xmls/China_Drone_000835.xml<br>China_Drone/train/annotations/xmls/China_Drone_001485.xml | EXCLUDE — 3 annotations only, not in challenge spec |
| D01 | 179 | India/train/annotations/xmls/India_005014.xml<br>India/train/annotations/xmls/India_005014.xml<br>India/train/annotations/xmls/India_003465.xml | EXCLUDE — variant of D00; covered by D00 |
| D0w0 | 1 | India/train/annotations/xmls/India_006389.xml | EXCLUDE — single annotation, likely labelling error |
| D11 | 45 | India/train/annotations/xmls/India_005835.xml<br>India/train/annotations/xmls/India_005835.xml<br>India/train/annotations/xmls/India_000145.xml | EXCLUDE — variant of D10; covered by D10 |
| D43 | 793 | Japan/train/annotations/xmls/Japan_004582.xml<br>Japan/train/annotations/xmls/Japan_008409.xml<br>Japan/train/annotations/xmls/Japan_007048.xml | EXCLUDE — unclear semantics, not in challenge spec |
| D44 | 5057 | Japan/train/annotations/xmls/Japan_000096.xml<br>Japan/train/annotations/xmls/Japan_000096.xml<br>Japan/train/annotations/xmls/Japan_006395.xml | EXCLUDE — unclear semantics, not in challenge spec |
| D50 | 3581 | Japan/train/annotations/xmls/Japan_004582.xml<br>Japan/train/annotations/xmls/Japan_011831.xml<br>Japan/train/annotations/xmls/Japan_011831.xml | EXCLUDE — unclear semantics, not in challenge spec |
| Repair | 1046 | China_Drone/train/annotations/xmls/China_Drone_000556.xml<br>China_Drone/train/annotations/xmls/China_Drone_001890.xml<br>China_Drone/train/annotations/xmls/China_Drone_001890.xml | EXCLUDE (for now) — future patch hard-negative class (Phase 2/3) |

## Missing Labels / Unlabelled
- Total images with no annotations: 20759

**Test sets with no labels (we will build our own labelled split):**
- Norway/test
- Czech/test
- China_MotorBike/test
- United_States/test
- Japan/test
- India/test

## Corrupted Images
- Total corrupted images: 0

## Size Distribution (top 10)
- 600x600: 16268 images
- 720x720: 9665 images
- 640x640: 6005 images
- 4040x2035: 5400 images
- 512x512: 4878 images
- 3643x2041: 3655 images
- 3650x2044: 1146 images
- 1024x1024: 191 images
- 540x540: 152 images
- 1080x1080: 60 images