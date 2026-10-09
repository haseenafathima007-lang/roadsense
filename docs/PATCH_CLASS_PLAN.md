# Patch Class Integration Plan

## Objective
To improve model robustness by adding a `patch` (repaired road/hard negative) class. Since RDD2022 does not include this class, we will integrate user-provided, self-labelled photos.

## Labelling Guide (CVAT)
We recommend using **CVAT (Computer Vision Annotation Tool)** due to its robust export capabilities (including direct YOLO format export).

### Steps:
1. **Setup:** Install CVAT locally or use cvat.ai.
2. **Project Creation:** Create a new project named "Road Patch Dataset".
3. **Labels:** Add a single label: `patch`.
4. **Upload:** Upload your original photos containing patched roads.
5. **Annotation:** Draw bounding boxes around all visible asphalt/concrete patches. 
6. **Export:** Export the dataset in **YOLO 1.1** format. This will yield images and `.txt` files where the class index is `0`.

## Merge Strategy (`scripts/merge_patch_class.py`)
Because our current `rdd3` scheme has classes `D00`, `D10`, `D20`, `D40` mapped to 0, 1, 2, 3 (where D00 and D10 are combined into 0, etc), the new `patch` class will be assigned index `4`.

The script will:
1. Iterate over the exported CVAT labels.
2. Remap the CVAT class index (`0` for patch) to `4`.
3. Copy the user images and rewritten labels into the main `data/processed/yolo_rdd3/` directory structure.
4. Update `data.yaml` to include `nc: 5` and add `patch` to the names list.

(A merge script is implemented in `scripts/merge_patch_class.py`.)
