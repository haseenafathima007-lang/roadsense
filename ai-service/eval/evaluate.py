#!/usr/bin/env python3
import argparse
import json
import logging
import subprocess
import time
from collections import defaultdict
from pathlib import Path

from ultralytics import YOLO

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

def get_git_commit():
    try:
        return subprocess.check_output(
            ['git', 'rev-parse', '--short', 'HEAD']
        ).decode('ascii').strip()
    except Exception:
        return "unknown"

def hash_file(path: Path):
    import hashlib
    if not path.exists():
        return None
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        while chunk := f.read(8192):
            h.update(chunk)
    return h.hexdigest()

def create_country_splits(test_txt_path, out_dir):
    out_dir.mkdir(parents=True, exist_ok=True)
    country_files = defaultdict(list)

    with open(test_txt_path, 'r') as f:
        lines = [line.strip() for line in f if line.strip()]

    yolo_test_images_dir = Path("data/processed/yolo_rdd3/test/images").absolute()
    resolved_paths = []

    for line in lines:
        path = Path(line)
        if yolo_test_images_dir.exists() and (yolo_test_images_dir / path.name).exists():
            resolved = (yolo_test_images_dir / path.name).absolute()
        else:
            resolved = path.absolute()

        resolved_paths.append(resolved.as_posix())
        parts = path.name.split('_')
        country = "_".join(parts[:-1]) if len(parts) > 1 else parts[0]
        country_files[country].append(resolved.as_posix())

    yaml_paths = {}
    overall_txt = out_dir / "temp_overall_test.txt"
    with open(overall_txt, 'w') as f:
        f.write("\n".join(resolved_paths) + "\n")

    for country, files in country_files.items():
        txt_path = out_dir / f"{country}_test.txt"
        with open(txt_path, 'w') as f:
            f.write("\n".join(files) + "\n")

        yaml_path = out_dir / f"{country}_test.yaml"
        with open(yaml_path, 'w') as f:
            f.write("path: .\n")
            f.write(f"train: {txt_path.absolute()}\n")
            f.write(f"val: {txt_path.absolute()}\n")
            f.write(f"test: {txt_path.absolute()}\n")
            f.write("nc: 3\n")
            f.write("names: ['crack_linear', 'alligator', 'pothole']\n")

        yaml_paths[country] = yaml_path

    return yaml_paths, overall_txt

def evaluate(model_path, dataset_yaml, run_name, project_dir):
    model = YOLO(model_path)
    metrics = model.val(
        data=dataset_yaml,
        project=project_dir,
        name=run_name,
        split='test',
        conf=0.25,
        iou=0.7,
        save_json=True,
        plots=True,
    )

    class_metrics = {}
    for i, name in enumerate(metrics.names.values()):
        if hasattr(metrics.box, 'ap50') and i < len(metrics.box.ap50):
            class_metrics[name] = {
                "precision": float(metrics.box.p[i]) if len(metrics.box.p) > i else 0.0,
                "recall": float(metrics.box.r[i]) if len(metrics.box.r) > i else 0.0,
                "mAP50": float(metrics.box.ap50[i]) if len(metrics.box.ap50) > i else 0.0,
                "mAP50_95": float(metrics.box.ap[i]) if len(metrics.box.ap) > i else 0.0,
            }

    return {
        "precision": float(metrics.box.mp),
        "recall": float(metrics.box.mr),
        "mAP50": float(metrics.box.map50),
        "mAP50_95": float(metrics.box.map),
        "per_class": class_metrics
    }

def main():
    default_model = "ai-service/training/runs/rdd3_nano_2hr/weights/best.pt"
    if not Path(default_model).exists():
        default_model = "ai-service/training/runs_smoke/rdd3_nano_2hr_smoke/weights/best.pt"

    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--model", type=str,
        default=default_model
    )
    parser.add_argument("--dataset", type=str, default="test", choices=["test", "chennai"])
    args = parser.parse_args()

    project_dir = Path("ai-service/runs/eval")
    project_dir.mkdir(parents=True, exist_ok=True)

    if args.dataset == "test":
        split_txt = Path("data/processed/splits/test.txt")
        main_yaml = "ai-service/training/configs/rdd3_nano.yaml" # Just for reference
        out_json = Path("docs/test_metrics.json")
    else:
        # Create a dummy split file for Chennai
        split_txt = Path("data/own/chennai/test.txt")
        if not split_txt.parent.exists():
            split_txt.parent.mkdir(parents=True, exist_ok=True)
        if not split_txt.exists():
            split_txt.touch()
        main_yaml = "chennai"
        out_json = Path("docs/chennai_ood_metrics.json")

    out_json.parent.mkdir(parents=True, exist_ok=True)

    results = {
        "metadata": {
            "model_hash": hash_file(Path(args.model)),
            "split_hash": hash_file(split_txt),
            "config": main_yaml,
            "git_commit": get_git_commit(),
            "date": time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
            "thresholds": {"conf": 0.25, "iou": 0.70}
        },
        "overall": {},
        "per_country": {}
    }

    if not Path(args.model).exists():
        logging.warning(f"Model {args.model} not found. Emitting empty metrics.")
        with open(out_json, "w") as f:
            json.dump(results, f, indent=4)
        return

    if args.dataset == "test" and split_txt.exists():
        # Overall eval
        yaml_paths, overall_txt = create_country_splits(split_txt, project_dir / "countries")

        temp_yaml = project_dir / "temp_test.yaml"
        with open(temp_yaml, 'w') as f:
            f.write("path: .\n")
            f.write(f"train: {overall_txt.absolute()}\n")
            f.write(f"val: {overall_txt.absolute()}\n")
            f.write(f"test: {overall_txt.absolute()}\n")
            f.write("nc: 3\n")
            f.write("names: ['crack_linear', 'alligator', 'pothole']\n")

        logging.info("Running overall evaluation...")
        results["overall"] = evaluate(args.model, str(temp_yaml), "test_overall", str(project_dir))

        # Per country eval
        for country, ypath in yaml_paths.items():
            logging.info(f"Running evaluation for {country}...")
            results["per_country"][country] = evaluate(
                args.model, str(ypath), f"test_{country}", str(project_dir)
            )
    elif args.dataset == "chennai":
        # Just run on chennai if files exist
        images = list(split_txt.parent.glob("*.jpg"))
        if len(images) > 0:
            with open(split_txt, 'w') as f:
                for img in images:
                    f.write(f"{img.absolute()}\n")
            temp_yaml = project_dir / "temp_chennai.yaml"
            with open(temp_yaml, 'w') as f:
                f.write("path: .\n")
                f.write(f"test: {split_txt.absolute()}\n")
                f.write("nc: 4\n")
                f.write("names: ['crack_linear', 'alligator', 'pothole', 'patch']\n")
            results["overall"] = evaluate(
                args.model, str(temp_yaml), "chennai_overall", str(project_dir)
            )
            results["per_country"]["Chennai"] = results["overall"]
        else:
            logging.info("No Chennai images found. Emitting empty metrics.")

    with open(out_json, "w") as f:
        json.dump(results, f, indent=4)

    logging.info(f"Evaluation complete. Metrics saved to {out_json}")

if __name__ == "__main__":
    main()
