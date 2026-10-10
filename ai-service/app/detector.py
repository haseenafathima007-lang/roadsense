import abc
import io

from PIL import Image

from app.config import settings
from app.schemas import Detection, DetectResponse, ImageInfo


class Detector(abc.ABC):
    @abc.abstractmethod
    def detect(self, image_data: bytes) -> DetectResponse:
        pass

class StubDetector(Detector):
    def detect(self, image_data: bytes) -> DetectResponse:
        try:
            image = Image.open(io.BytesIO(image_data))
            image.verify()
        except Exception:
            raise ValueError("Invalid image")

        return DetectResponse(
            image=ImageInfo(w=640, h=480),
            detections=[
                Detection(**{"class": "D00", "conf": 0.85, "bbox": [10, 10, 100, 100]})
            ]
        )

class YoloDetector(Detector):
    def __init__(self, weights_path: str):
        from ultralytics import YOLO
        self.model = YOLO(weights_path)

    def detect(self, image_data: bytes) -> DetectResponse:
        image = Image.open(io.BytesIO(image_data))
        # convert to rgb if needed
        if image.mode != "RGB":
            image = image.convert("RGB")
        w, h = image.size

        results = self.model.predict(image, verbose=False)

        detections = []
        for r in results:
            boxes = r.boxes
            for box in boxes:
                cls_id = int(box.cls[0].item())
                cls_name = r.names[cls_id]
                conf = float(box.conf[0].item())
                xyxy = box.xyxy[0].tolist()

                detections.append(Detection(**{
                    "class": cls_name,
                    "conf": conf,
                    "bbox": xyxy
                }))

        return DetectResponse(
            image=ImageInfo(w=w, h=h),
            detections=detections
        )

# Dependency injection
def get_detector() -> Detector:
    # We can inject stub in tests by overriding this dependency
    return YoloDetector(settings.WEIGHTS_PATH)
