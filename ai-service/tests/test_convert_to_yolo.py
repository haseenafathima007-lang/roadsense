"""Tests for convert_to_yolo.py using tiny synthetic fixtures."""

import importlib.util
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image

# ── Load module ───────────────────────────────────────────────────────────────

_CONV_MOD_PATH = Path(__file__).parent.parent / "training" / "data" / "convert_to_yolo.py"


def _load_conv_module(name: str = "convert_to_yolo"):
    spec = importlib.util.spec_from_file_location(name, _CONV_MOD_PATH)
    mod = importlib.util.module_from_spec(spec)
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


# ── Helpers ───────────────────────────────────────────────────────────────────


def _make_image(path: Path, w: int = 600, h: int = 400) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    Image.new("RGB", (w, h), color=(128, 0, 0)).save(path)


def _make_xml(
    path: Path,
    classes: list[str],
    boxes: list[tuple[int, int, int, int]],
    img_w: int = 600,
    img_h: int = 400,
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    root = ET.Element("annotation")
    size_el = ET.SubElement(root, "size")
    ET.SubElement(size_el, "width").text = str(img_w)
    ET.SubElement(size_el, "height").text = str(img_h)
    for cls, (xmin, ymin, xmax, ymax) in zip(classes, boxes):
        obj = ET.SubElement(root, "object")
        ET.SubElement(obj, "name").text = cls
        bb = ET.SubElement(obj, "bndbox")
        ET.SubElement(bb, "xmin").text = str(xmin)
        ET.SubElement(bb, "ymin").text = str(ymin)
        ET.SubElement(bb, "xmax").text = str(xmax)
        ET.SubElement(bb, "ymax").text = str(ymax)
    ET.ElementTree(root).write(path)


def _make_split_txt(path: Path, img_paths: list[Path]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(str(p) for p in img_paths) + "\n")


# ── Tests ─────────────────────────────────────────────────────────────────────


def test_convert_box_normal() -> None:
    """Normal box converts correctly to YOLO format."""
    cv = _load_conv_module("cv_box")
    cx, cy, bw, bh = cv.convert_box(100, 50, 300, 200, img_w=600, img_h=400)
    assert abs(cx - (100 + 300) / 2 / 600) < 1e-6
    assert abs(cy - (50 + 200) / 2 / 400) < 1e-6
    assert abs(bw - 200 / 600) < 1e-6
    assert abs(bh - 150 / 400) < 1e-6


def test_convert_box_clips_out_of_bounds() -> None:
    """Box extending beyond image boundaries is clipped, not None."""
    cv = _load_conv_module("cv_clip")
    result = cv.convert_box(-10, -10, 700, 500, img_w=600, img_h=400)
    assert result is not None, "Expected clipped box, not None"
    cx, cy, bw, bh = result
    assert 0.0 <= cx <= 1.0
    assert 0.0 <= cy <= 1.0
    assert 0.0 < bw <= 1.0
    assert 0.0 < bh <= 1.0


def test_convert_box_degenerate_returns_none() -> None:
    """Zero-area box after clipping returns None."""
    cv = _load_conv_module("cv_degen")
    result = cv.convert_box(100, 100, 100, 100, img_w=600, img_h=400)
    assert result is None


def test_rdd4_class_ids(tmp_path: Path) -> None:
    """rdd4 scheme assigns correct class IDs 0-3."""
    cv = _load_conv_module("cv_rdd4")

    raw = tmp_path / "data" / "raw" / "RDD2022"
    img_path = raw / "Japan" / "train" / "images" / "Japan_001.jpg"
    _make_image(img_path)
    _make_xml(
        raw / "Japan" / "train" / "annotations" / "xmls" / "Japan_001.xml",
        classes=["D00", "D10", "D20", "D40"],
        boxes=[(10, 10, 100, 100)] * 4,
    )

    splits_dir = tmp_path / "data" / "processed" / "splits"
    _make_split_txt(splits_dir / "train.txt", [img_path])

    out_dir = tmp_path / "data" / "processed" / "yolo_rdd4"
    (out_dir / "train" / "images").mkdir(parents=True, exist_ok=True)
    (out_dir / "train" / "labels").mkdir(parents=True, exist_ok=True)
    drop_log: list[str] = []
    cv.process_split(
        split_txt=splits_dir / "train.txt",
        raw_dir=raw,
        out_images=out_dir / "train" / "images",
        out_labels=out_dir / "train" / "labels",
        class_map=cv.SCHEMES["rdd4"],
        drop_log=drop_log,
        use_symlinks=False,
    )

    lbl = (out_dir / "train" / "labels" / "Japan_001.txt").read_text().strip().splitlines()
    cls_ids = [int(line.split()[0]) for line in lbl]
    assert cls_ids == [0, 1, 2, 3], f"Expected [0,1,2,3], got {cls_ids}"
    assert not drop_log, f"Unexpected drops: {drop_log}"


def test_rdd3_merges_D00_D10(tmp_path: Path) -> None:
    """rdd3 scheme merges D00 and D10 to class id 0 (crack_linear)."""
    cv = _load_conv_module("cv_rdd3")

    raw = tmp_path / "data" / "raw" / "RDD2022"
    img_path = raw / "Japan" / "train" / "images" / "Japan_002.jpg"
    _make_image(img_path)
    _make_xml(
        raw / "Japan" / "train" / "annotations" / "xmls" / "Japan_002.xml",
        classes=["D00", "D10", "D20", "D40"],
        boxes=[(10, 10, 100, 100)] * 4,
    )

    splits_dir = tmp_path / "data" / "processed" / "splits"
    _make_split_txt(splits_dir / "train.txt", [img_path])

    out_dir = tmp_path / "data" / "processed" / "yolo_rdd3"
    (out_dir / "train" / "images").mkdir(parents=True, exist_ok=True)
    (out_dir / "train" / "labels").mkdir(parents=True, exist_ok=True)
    drop_log: list[str] = []
    cv.process_split(
        split_txt=splits_dir / "train.txt",
        raw_dir=raw,
        out_images=out_dir / "train" / "images",
        out_labels=out_dir / "train" / "labels",
        class_map=cv.SCHEMES["rdd3"],
        drop_log=drop_log,
        use_symlinks=False,
    )

    lbl = (out_dir / "train" / "labels" / "Japan_002.txt").read_text().strip().splitlines()
    cls_ids = [int(line.split()[0]) for line in lbl]
    # D00→0, D10→0, D20→1, D40→2
    assert cls_ids == [0, 0, 1, 2], f"Expected [0,0,1,2], got {cls_ids}"


def test_excluded_class_is_dropped(tmp_path: Path) -> None:
    """Non-standard class D44 is logged in drop_log and excluded from label file."""
    cv = _load_conv_module("cv_excl")

    raw = tmp_path / "data" / "raw" / "RDD2022"
    img_path = raw / "Japan" / "train" / "images" / "Japan_003.jpg"
    _make_image(img_path)
    _make_xml(
        raw / "Japan" / "train" / "annotations" / "xmls" / "Japan_003.xml",
        classes=["D00", "D44"],
        boxes=[(10, 10, 100, 100)] * 2,
    )

    splits_dir = tmp_path / "data" / "processed" / "splits"
    _make_split_txt(splits_dir / "train.txt", [img_path])
    out_dir = tmp_path / "data" / "processed" / "yolo_rdd4"
    (out_dir / "train" / "images").mkdir(parents=True, exist_ok=True)
    (out_dir / "train" / "labels").mkdir(parents=True, exist_ok=True)

    drop_log: list[str] = []
    cv.process_split(
        split_txt=splits_dir / "train.txt",
        raw_dir=raw,
        out_images=out_dir / "train" / "images",
        out_labels=out_dir / "train" / "labels",
        class_map=cv.SCHEMES["rdd4"],
        drop_log=drop_log,
        use_symlinks=False,
    )

    lbl = (out_dir / "train" / "labels" / "Japan_003.txt").read_text().strip().splitlines()
    cls_ids = [int(line.split()[0]) for line in lbl]
    assert cls_ids == [0], "D00 kept; D44 excluded"
    assert any("D44" in entry for entry in drop_log), "D44 not in drop_log"


def test_boxes_normalised_in_unit_range(tmp_path: Path) -> None:
    """All box coordinates in label files are in [0, 1]."""
    cv = _load_conv_module("cv_norm")

    raw = tmp_path / "data" / "raw" / "RDD2022"
    img_path = raw / "Norway" / "train" / "images" / "Norway_001.jpg"
    _make_image(img_path, w=600, h=400)
    _make_xml(
        raw / "Norway" / "train" / "annotations" / "xmls" / "Norway_001.xml",
        classes=["D00"],
        boxes=[(0, 0, 599, 399)],
        img_w=600,
        img_h=400,
    )

    splits_dir = tmp_path / "data" / "processed" / "splits"
    _make_split_txt(splits_dir / "train.txt", [img_path])
    out_dir = tmp_path / "data" / "processed" / "yolo_rdd4"
    (out_dir / "train" / "images").mkdir(parents=True, exist_ok=True)
    (out_dir / "train" / "labels").mkdir(parents=True, exist_ok=True)

    drop_log: list[str] = []
    cv.process_split(
        split_txt=splits_dir / "train.txt",
        raw_dir=raw,
        out_images=out_dir / "train" / "images",
        out_labels=out_dir / "train" / "labels",
        class_map=cv.SCHEMES["rdd4"],
        drop_log=drop_log,
        use_symlinks=False,
    )

    lbl_line = (out_dir / "train" / "labels" / "Norway_001.txt").read_text().strip()
    parts = lbl_line.split()
    assert len(parts) == 5
    cx, cy, bw, bh = float(parts[1]), float(parts[2]), float(parts[3]), float(parts[4])
    for val in (cx, cy, bw, bh):
        assert 0.0 <= val <= 1.0, f"Out-of-range coordinate: {val}"
