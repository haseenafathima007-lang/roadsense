"""Tests for inventory.py using tiny synthetic fixtures (no real dataset required)."""

import json
import xml.etree.ElementTree as ET
from pathlib import Path

import pytest
from PIL import Image

# ── Helpers ───────────────────────────────────────────────────────────────────


def _make_image(path: Path, size: tuple[int, int] = (600, 400)) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGB", size, color=(128, 128, 128))
    img.save(path)


def _make_xml(
    path: Path,
    classes: list[str],
    img_w: int = 600,
    img_h: int = 400,
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    root = ET.Element("annotation")
    size_el = ET.SubElement(root, "size")
    ET.SubElement(size_el, "width").text = str(img_w)
    ET.SubElement(size_el, "height").text = str(img_h)
    for cls in classes:
        obj = ET.SubElement(root, "object")
        ET.SubElement(obj, "name").text = cls
        bb = ET.SubElement(obj, "bndbox")
        ET.SubElement(bb, "xmin").text = "10"
        ET.SubElement(bb, "ymin").text = "10"
        ET.SubElement(bb, "xmax").text = "100"
        ET.SubElement(bb, "ymax").text = "100"
    ET.ElementTree(root).write(path)


# ── Fixtures ──────────────────────────────────────────────────────────────────


@pytest.fixture()
def fake_dataset(tmp_path: Path) -> Path:
    """
    Minimal synthetic RDD2022 layout:

      FakeCountry/
        train/images/img_001.jpg  →  D00, D10
        train/images/img_002.jpg  →  D44 (excluded)
        train/images/img_003.jpg  →  no XML (unlabelled)
        train/annotations/xmls/img_001.xml
        train/annotations/xmls/img_002.xml
        test/images/img_t01.jpg   →  no annotations dir (unlabelled test)
    """
    raw = tmp_path / "data" / "raw" / "RDD2022"

    # train images
    _make_image(raw / "FakeCountry" / "train" / "images" / "img_001.jpg")
    _make_image(raw / "FakeCountry" / "train" / "images" / "img_002.jpg")
    _make_image(raw / "FakeCountry" / "train" / "images" / "img_003.jpg")

    # XMLs
    _make_xml(
        raw / "FakeCountry" / "train" / "annotations" / "xmls" / "img_001.xml",
        classes=["D00", "D10"],
    )
    _make_xml(
        raw / "FakeCountry" / "train" / "annotations" / "xmls" / "img_002.xml",
        classes=["D44"],
    )

    # test images — no annotations dir
    _make_image(raw / "FakeCountry" / "test" / "images" / "img_t01.jpg")

    return tmp_path


# ── Tests ─────────────────────────────────────────────────────────────────────


def test_inventory_counts(fake_dataset: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    """inventory.py counts images, annotations, and excluded classes correctly."""
    import importlib.util
    import sys

    monkeypatch.chdir(fake_dataset)

    spec = importlib.util.spec_from_file_location(
        "inventory",
        Path(__file__).parent.parent / "training" / "data" / "inventory.py",
    )
    inv = importlib.util.module_from_spec(spec)
    sys.modules["inventory"] = inv
    spec.loader.exec_module(inv)
    inv.main()

    audit_path = fake_dataset / "docs" / "data_audit.json"
    assert audit_path.exists(), "data_audit.json not written"

    with open(audit_path) as f:
        audit = json.load(f)

    # Image counts
    assert audit["images_per_split"]["FakeCountry/train"] == 3
    assert audit["images_per_split"]["FakeCountry/test"] == 1

    # Standard class annotations
    assert audit["annotations_per_class_country"]["FakeCountry"]["D00"] == 1
    assert audit["annotations_per_class_country"]["FakeCountry"]["D10"] == 1

    # Excluded class
    assert "D44" in audit["excluded_classes_audit"]
    assert audit["excluded_classes_audit"]["D44"]["count"] == 1

    # Unlabelled: img_003 (no XML), img_t01 (test, no annots dir) = 2 unlabelled in train
    # img_002 has only excluded class D44 → also counted as unlabelled
    assert len(audit["unlabelled_images"]) >= 2

    # Test sets with no labels
    assert "FakeCountry/test" in audit["test_sets_missing_labels"]


def test_inventory_no_corruption(fake_dataset: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    """No corrupted images in a clean fixture."""
    import importlib.util
    import sys

    monkeypatch.chdir(fake_dataset)

    spec = importlib.util.spec_from_file_location(
        "inventory2",
        Path(__file__).parent.parent / "training" / "data" / "inventory.py",
    )
    inv = importlib.util.module_from_spec(spec)
    sys.modules["inventory2"] = inv
    spec.loader.exec_module(inv)
    inv.main()

    with open(fake_dataset / "docs" / "data_audit.json") as f:
        audit = json.load(f)

    assert len(audit["corrupted_images"]) == 0


def test_data_audit_md_matches_json(fake_dataset: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    """All numbers in data_audit.md appear in data_audit.json."""
    import importlib.util
    import re
    import sys

    monkeypatch.chdir(fake_dataset)

    spec = importlib.util.spec_from_file_location(
        "inventory3",
        Path(__file__).parent.parent / "training" / "data" / "inventory.py",
    )
    inv = importlib.util.module_from_spec(spec)
    sys.modules["inventory3"] = inv
    spec.loader.exec_module(inv)
    inv.main()

    with open(fake_dataset / "docs" / "data_audit.json") as f:
        audit = json.load(f)

    md = (fake_dataset / "docs" / "data_audit.md").read_text()

    # Unlabelled count
    m = re.search(r"Total images with no annotations: (\d+)", md)
    assert m, "unlabelled count missing from MD"
    assert int(m.group(1)) == len(audit["unlabelled_images"])

    # Corrupted count
    m = re.search(r"Total corrupted images: (\d+)", md)
    assert m, "corrupted count missing from MD"
    assert int(m.group(1)) == len(audit["corrupted_images"])
