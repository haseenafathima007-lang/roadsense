from typing import List

from fastapi import Depends, FastAPI, File, HTTPException, UploadFile, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.auth import verify_api_key
from app.config import settings
from app.detector import Detector, get_detector
from app.schemas import BatchItem, DetectResponse

app = FastAPI(title="Road Damage Intelligence Framework - Detection Service")

MAX_UPLOAD_BYTES = settings.MAX_UPLOAD_MB * 1024 * 1024
ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png"}

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    if hasattr(exc.detail, "get") and "error" in exc.detail:
        return JSONResponse(status_code=exc.status_code, content=exc.detail)
    return JSONResponse(
        status_code=exc.status_code,
        content={"error": {"code": "HTTP_ERROR", "message": str(exc.detail)}}
    )

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request, exc):
    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content={"error": {"code": "BAD_REQUEST", "message": "Invalid request parameters"}}
    )

@app.get("/health")
def health():
    return {
        "status": "ok",
        "model": "yolov8",
        "scheme": settings.SCHEME
    }

async def validate_image(file: UploadFile) -> bytes:
    if file.content_type not in ALLOWED_CONTENT_TYPES:
        raise HTTPException(
            status_code=status.HTTP_415_UNSUPPORTED_MEDIA_TYPE,
            detail={
                "error": {
                    "code": "UNSUPPORTED_MEDIA_TYPE",
                    "message": "Allowed types are image/jpeg, image/png",
                }
            }
        )

    image_data = await file.read()
    if len(image_data) > MAX_UPLOAD_BYTES:
        raise HTTPException(
            status_code=status.HTTP_413_CONTENT_TOO_LARGE,
            detail={
                "error": {
                    "code": "PAYLOAD_TOO_LARGE",
                    "message": f"Max upload size is {settings.MAX_UPLOAD_MB}MB",
                }
            }
        )
    return image_data

@app.post("/v1/detect/image", response_model=DetectResponse, dependencies=[Depends(verify_api_key)])
async def detect_image(
    file: UploadFile = File(...),
    detector: Detector = Depends(get_detector)
):
    image_data = await validate_image(file)
    try:
        return detector.detect(image_data)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail={"error": {"code": "INTERNAL_ERROR", "message": str(e)}}
        )

@app.post(
    "/v1/detect/batch",
    response_model=List[BatchItem],
    dependencies=[Depends(verify_api_key)]
)
async def detect_batch(
    files: List[UploadFile] = File(...),
    detector: Detector = Depends(get_detector)
):
    results = []
    for file in files:
        try:
            image_data = await validate_image(file)
            res = detector.detect(image_data)
            results.append(BatchItem(
                filename=file.filename,
                image=res.image,
                detections=res.detections
            ))
        except HTTPException as e:
            # Re-raise HTTP exceptions immediately to abort batch or could handle per file.
            # Given CONTRACTS.md we'll just fail the whole request if one is too big or bad type.
            raise e
        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail={"error": {"code": "INTERNAL_ERROR", "message": str(e)}}
            )
    return results
