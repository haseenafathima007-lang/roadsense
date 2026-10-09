import argparse
import hashlib
import json
import platform
import subprocess
import sys
import time
from pathlib import Path

import torch
import ultralytics
import yaml
from ultralytics import YOLO


def get_git_commit():
    try:
        return subprocess.check_output(["git", "rev-parse", "HEAD"]).decode("ascii").strip()
    except Exception:
        return "unknown"

def hash_file(filepath):
    if not filepath.exists():
        return "not_found"
    sha = hashlib.sha256()
    with open(filepath, "rb") as f:
        while chunk := f.read(8192):
            sha.update(chunk)
    return sha.hexdigest()

def train(config_path, resume=False, smoke=False):
    base_dir = Path(__file__).resolve().parent.parent.parent

    with open(config_path) as f:
        cfg = yaml.safe_load(f)

    data_yaml_path = base_dir / cfg["data"]
    if not data_yaml_path.is_absolute():
        data_yaml_path = base_dir / cfg["data"]

    project_dir = base_dir / "ai-service" / "training" / cfg.get("project", "runs")
    run_name = cfg.get("name", "train_run")
    if smoke:
        run_name += "_smoke"
        project_dir = project_dir.parent / "runs_smoke"

    run_dir = project_dir / run_name

    if resume:
        # Check last checkpoint
        weights_dir = run_dir / "weights"
        last_pt = weights_dir / "last.pt"
        if last_pt.exists():
            print(f"Resuming from {last_pt}")
            model = YOLO(str(last_pt))
            # In Ultralytics, resume=True will automatically continue from model weights
            model.train(resume=True)
            return
        else:
            print(f"Cannot resume: {last_pt} not found. Starting fresh.")

    # Fresh start
    model = YOLO(cfg["model"])

    train_args = {
        "data": str(data_yaml_path),
        "epochs": cfg.get("epochs", 1),
        "imgsz": cfg.get("imgsz", 640),
        "batch": cfg.get("batch", 16),
        "device": cfg.get("device", "cpu"),
        "project": str(project_dir),
        "name": run_name,
        "seed": cfg.get("seed", 42),
        "save_period": 1, # checkpoint every epoch
        "exist_ok": True
    }
    if "fraction" in cfg:
        train_args["fraction"] = float(cfg["fraction"])

    if smoke:
        train_args["epochs"] = 1
        # ~50 images if dataset is 1000, for our dataset ~1300 but close enough for smoke
        train_args["fraction"] = 0.05

    start_time = time.time()
    start_time_str = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime(start_time))

    print("Starting training...")
    try:
        model.train(**train_args)
    except KeyboardInterrupt:
        print("\nTraining interrupted by user. You can resume using --resume flag.")

    end_time = time.time()
    end_time_str = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime(end_time))

    # Write run_info.json
    splits_dir = data_yaml_path.parent.parent / "splits"
    train_txt = splits_dir / "train.txt"
    val_txt = splits_dir / "val.txt"

    run_info = {
        "start_time": start_time_str,
        "end_time": end_time_str,
        "duration_seconds": end_time - start_time,
        "hardware": {
            "os": f"{platform.system()} {platform.machine()}",
            "device_requested": train_args["device"]
        },
        "versions": {
            "python": sys.version.split()[0],
            "torch": torch.__version__,
            "ultralytics": ultralytics.__version__
        },
        "git_commit": get_git_commit(),
        "dataset_hashes": {
            "train.txt": hash_file(train_txt),
            "val.txt": hash_file(val_txt)
        },
        "command_line": " ".join(sys.argv),
        "config": cfg,
        "smoke": smoke
    }

    run_dir.mkdir(parents=True, exist_ok=True)
    with open(run_dir / "run_info.json", "w") as f:
        json.dump(run_info, f, indent=4)

    print(f"Run info written to {run_dir / 'run_info.json'}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--config", type=str, default="ai-service/training/configs/rdd3_nano.yaml",
        help="Path to config YAML"
    )
    parser.add_argument("--resume", action="store_true", help="Resume from last checkpoint")
    parser.add_argument("--smoke", action="store_true", help="Smoke mode: 1 epoch on tiny subset")

    args = parser.parse_args()

    train(args.config, args.resume, args.smoke)
