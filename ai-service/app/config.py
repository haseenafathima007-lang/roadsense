import os


class Settings:
    MAX_UPLOAD_MB = int(os.environ.get("MAX_UPLOAD_MB", "15"))
    WEIGHTS_PATH = os.environ.get("WEIGHTS_PATH", "runs/train/rdd3_nano_2hr/weights/best.pt")
    DETECTOR_API_KEY = os.environ.get("DETECTOR_API_KEY", "test-key-123")
    SCHEME = os.environ.get("SCHEME", "rdd3")

settings = Settings()
