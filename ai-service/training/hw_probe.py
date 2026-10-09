import platform
import shutil
import sys

import psutil
import torch
import ultralytics


def probe_hardware():
    print("=== Hardware & Environment Probe ===")

    # OS
    print(f"OS: {platform.system()} {platform.machine()} ({platform.release()})")

    # Python and Libraries
    print(f"Python version: {sys.version.split()[0]}")
    print(f"PyTorch version: {torch.__version__}")
    print(f"Ultralytics version: {ultralytics.__version__}")

    # Device detection
    device_str = "cpu"
    accelerator = "None"

    if torch.cuda.is_available():
        device_str = "cuda:0"
        accelerator = f"CUDA GPU: {torch.cuda.get_device_name(0)}"
    elif torch.backends.mps.is_available():
        device_str = "mps"
        accelerator = "Apple MPS (Metal Performance Shaders)"

    print(f"Accelerator detected: {accelerator}")
    print(f"Device string for YOLO: '{device_str}'")

    # RAM
    ram = psutil.virtual_memory()
    total_ram_gb = ram.total / (1024**3)
    available_ram_gb = ram.available / (1024**3)
    print(f"System RAM: {total_ram_gb:.1f} GB total, {available_ram_gb:.1f} GB available")

    # Disk
    disk = shutil.disk_usage("/")
    free_disk_gb = disk.free / (1024**3)
    print(f"Free Disk Space: {free_disk_gb:.1f} GB")

    # Verdict
    print("\n=== Verdict ===")
    if device_str == "mps":
        print("Verdict: Apple Silicon detected! Training will be accelerated via MPS.")
    elif device_str.startswith("cuda"):
        print("Verdict: NVIDIA GPU detected! Training will be accelerated via CUDA.")
    else:
        print("Verdict: No GPU detected. Training will run on CPU and may be slow.")

    print(f"Use device='{device_str}' in your training configurations.")

if __name__ == "__main__":
    probe_hardware()
