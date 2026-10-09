import sys
from pathlib import Path

import yaml

# Add scripts to path to import merge_patch_class
sys.path.append(str(Path(__file__).resolve().parent.parent.parent / "scripts"))
from merge_patch_class import merge_patch_data


def test_merge_patch_synthetic_fixtures(tmp_path):
    # Setup CVAT export dummy
    cvat_dir = tmp_path / "cvat_export"
    (cvat_dir / "images" / "train").mkdir(parents=True)
    (cvat_dir / "labels" / "train").mkdir(parents=True)

    # Dummy image
    (cvat_dir / "images" / "train" / "patch1.jpg").write_text("dummy image data")

    # Dummy CVAT label (class 0)
    (cvat_dir / "labels" / "train" / "patch1.txt").write_text("0 0.5 0.5 0.2 0.2\n")

    # Setup target dummy
    target_dir = tmp_path / "yolo_rdd3"
    (target_dir / "images" / "train").mkdir(parents=True)
    (target_dir / "labels" / "train").mkdir(parents=True)

    # Dummy existing target data
    (target_dir / "images" / "train" / "rdd_img1.jpg").write_text("dummy rdd data")
    (target_dir / "labels" / "train" / "rdd_img1.txt").write_text("1 0.1 0.1 0.1 0.1\n")

    # Dummy data.yaml
    data_yaml = {
        "path": "dummy",
        "train": "images/train",
        "val": "images/val",
        "nc": 4,
        "names": ["D00", "D10", "D20", "D40"]
    }
    with open(target_dir / "data.yaml", "w") as f:
        yaml.safe_dump(data_yaml, f)

    # Run merge
    merge_patch_data(cvat_dir, target_dir)

    # Assertions
    assert (target_dir / "images" / "train" / "patch1.jpg").exists(), "Patch image not copied"
    assert (target_dir / "images" / "train" / "rdd_img1.jpg").exists(), "Existing image lost"

    patch_label = (target_dir / "labels" / "train" / "patch1.txt").read_text().strip()
    assert patch_label == "4 0.5 0.5 0.2 0.2", f"Patch label not remapped to 4: {patch_label}"

    with open(target_dir / "data.yaml", "r") as f:
        updated_data = yaml.safe_load(f)

    assert updated_data["nc"] == 5
    assert "patch" in updated_data["names"]
    assert len(updated_data["names"]) == 5
