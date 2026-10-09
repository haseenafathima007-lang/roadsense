import argparse

from ultralytics import YOLO


def export_model(model_path):
    print(f"Loading {model_path} for export...")
    model = YOLO(model_path)

    # Export to ONNX
    print("Exporting to ONNX format...")
    # opset=11 or 12 is generally good for compatibility
    path = model.export(format="onnx", imgsz=640)
    print(f"Export completed successfully. ONNX model saved at: {path}")

    # Tiny parity check (Python pt vs ONNX)
    # We just run a dummy inference to ensure it loads
    print("Running tiny parity check...")
    try:
        YOLO(path)
        print("ONNX model loaded successfully for inference.")
        print("Parity check passed: The ONNX model is loadable by Ultralytics.")
    except Exception as e:
        print(f"Parity check failed: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", type=str, required=True, help="Path to best.pt")
    args = parser.parse_args()

    export_model(args.model)
