import time
from pathlib import Path

from ultralytics import YOLO


def run_benchmark():
    print("=== Training Benchmark (Estimate) ===")

    # Paths
    base_dir = Path(__file__).resolve().parent.parent.parent
    yolo_data_yaml = base_dir / "data" / "processed" / "yolo_rdd3" / "data.yaml"
    benchmark_dir = base_dir / "data" / "processed" / "benchmark"
    benchmark_dir.mkdir(parents=True, exist_ok=True)

    benchmark_dir / "benchmark_data.yaml"

    # We create a dummy dataset YAML pointing to the real images but we'll limit the number
    # of images during training via the 'fraction' parameter in Ultralytics YOLO

    # Use yolov8n.pt as baseline
    model = YOLO('yolov8n.pt')

    # We will train for 1 epoch on a fraction of data to measure time.
    # 5% of 26871 images is ~1343 images.
    fraction = 0.05
    epochs = 1

    print(f"Running benchmark on ~{fraction*100}% of the dataset for {epochs} epoch(s)...")

    start_time = time.time()

    # Suppress YOLO output for benchmark
    model.train(
        data=str(yolo_data_yaml),
        epochs=epochs,
        imgsz=640,
        device="mps", # using MPS from hw_probe
        fraction=fraction,
        project=str(benchmark_dir / "runs"),
        name="bench",
        verbose=False,
        val=False # Skip validation for pure training benchmark
    )

    end_time = time.time()
    elapsed_time = end_time - start_time

    print(f"\nMeasured time for {fraction*100}% of 1 epoch: {elapsed_time:.2f} seconds")

    # Extrapolation
    estimated_full_epoch_time = elapsed_time / fraction
    estimated_50_epochs = estimated_full_epoch_time * 50

    print("\n=== Extrapolated Estimates ===")
    print(f"Estimated time per full epoch: {estimated_full_epoch_time/60:.2f} minutes")
    print(f"Estimated time for 50 epochs: {estimated_50_epochs/3600:.2f} hours")
    print("\nNote: This is an ESTIMATE based on a small run. Actual times may vary "
          "due to validation, checkpointing, and GPU thermal throttling.")

if __name__ == "__main__":
    run_benchmark()
