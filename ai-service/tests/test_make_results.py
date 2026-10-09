import json
import sys
from pathlib import Path

sys.path.append(str(Path(__file__).parent.parent.parent / "scripts"))
from make_results import extract_exif_audit, extract_yolo_metrics, generate_results


def test_extract_yolo_metrics_missing_file(tmp_path):
    val, status = extract_yolo_metrics(tmp_path / "missing.json")
    assert val == ""
    assert status == "NOT YET MEASURED"

def test_extract_yolo_metrics_empty_file(tmp_path):
    file = tmp_path / "empty.json"
    file.write_text("{}")
    val, status = extract_yolo_metrics(file)
    assert val == ""
    assert status == "NOT YET MEASURED"

def test_extract_yolo_metrics_valid_file(tmp_path):
    file = tmp_path / "valid.json"
    data = {
        "overall": {
            "precision": 0.85,
            "recall": 0.90,
            "mAP50": 0.92
        }
    }
    file.write_text(json.dumps(data))
    val, status = extract_yolo_metrics(file)
    assert "P: 0.850" in val
    assert "R: 0.900" in val
    assert "mAP@.5: 0.920" in val
    assert status == "MEASURED"

def test_extract_exif_audit_missing_file(tmp_path):
    val, status = extract_exif_audit(tmp_path / "missing.json")
    assert val == ""
    assert status == "NOT YET MEASURED"

def test_extract_exif_audit_valid_file(tmp_path):
    file = tmp_path / "exif.json"
    data = {
        "total_images": 100,
        "usable_gps": 85
    }
    file.write_text(json.dumps(data))
    val, status = extract_exif_audit(file)
    assert val == "85/100 (85.0%)"
    assert status == "MEASURED"

def test_extract_exif_audit_zero_images(tmp_path):
    file = tmp_path / "exif_zero.json"
    file.write_text(json.dumps({"total_images": 0, "usable_gps": 0}))
    val, status = extract_exif_audit(file)
    assert val == ""
    assert status == "NOT YET MEASURED"


def test_generate_results(tmp_path):
    # Setup dummy JSONs
    docs_dir = tmp_path / "docs"
    docs_dir.mkdir()

    test_json = docs_dir / "test_metrics.json"
    test_json.write_text(json.dumps({"overall": {"precision": 0.8, "recall": 0.8, "mAP50": 0.8}}))

    exif_json = docs_dir / "exif_audit.json"
    exif_json.write_text(json.dumps({"total_images": 10, "usable_gps": 5}))

    # Missing chennai_ood_metrics.json to test the NOT YET MEASURED fallback

    generate_results(docs_dir)

    results_md = (docs_dir / "RESULTS.md").read_text()

    assert "P: 0.800, R: 0.800, mAP@.5: 0.800 | MEASURED" in results_md
    assert "| docs/chennai_ood_metrics.json |  | NOT YET MEASURED |" in results_md
    assert "5/10 (50.0%) | MEASURED" in results_md
