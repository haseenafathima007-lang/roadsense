from typing import List

from pydantic import BaseModel, Field


class ImageInfo(BaseModel):
    w: int
    h: int

class Detection(BaseModel):
    class_: str = Field(alias="class")  # Python keyword workaround
    conf: float
    bbox: List[float]  # [x1, y1, x2, y2]

class DetectResponse(BaseModel):
    image: ImageInfo
    detections: List[Detection]

class BatchItem(BaseModel):
    filename: str
    image: ImageInfo
    detections: List[Detection]

class ErrorDetail(BaseModel):
    code: str
    message: str

class ErrorBody(BaseModel):
    error: ErrorDetail
