import io

from fastapi.testclient import TestClient
from PIL import Image

from app.config import settings
from app.detector import StubDetector, get_detector
from app.main import app

client = TestClient(app)

# Override dependency to use StubDetector for tests
app.dependency_overrides[get_detector] = lambda: StubDetector()

def generate_test_image(format="JPEG", size=(100, 100)) -> bytes:
    img = Image.new("RGB", size, color="white")
    buf = io.BytesIO()
    img.save(buf, format=format)
    return buf.getvalue()

def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"

def test_auth_fail():
    response = client.post(
        "/v1/detect/image",
        files={"file": ("test.jpg", b"dummy", "image/jpeg")}
    )
    assert response.status_code == 401
    assert "error" in response.json()
    assert response.json()["error"]["code"] == "UNAUTHORIZED"

    response = client.post(
        "/v1/detect/image",
        headers={"X-API-Key": "wrong-key"},
        files={"file": ("test.jpg", b"dummy", "image/jpeg")}
    )
    assert response.status_code == 401

def test_wrong_mime_type():
    headers = {"X-API-Key": settings.DETECTOR_API_KEY}
    response = client.post(
        "/v1/detect/image",
        headers=headers,
        files={"file": ("test.txt", b"dummy text", "text/plain")}
    )
    assert response.status_code == 415
    assert response.json()["error"]["code"] == "UNSUPPORTED_MEDIA_TYPE"

def test_oversize_upload(monkeypatch):
    # Temporarily set max size to 1 byte for test
    monkeypatch.setattr("app.main.MAX_UPLOAD_BYTES", 1)

    headers = {"X-API-Key": settings.DETECTOR_API_KEY}
    response = client.post(
        "/v1/detect/image",
        headers=headers,
        files={"file": ("test.jpg", b"dummy", "image/jpeg")}
    )
    assert response.status_code == 413
    assert response.json()["error"]["code"] == "PAYLOAD_TOO_LARGE"

def test_happy_path_detect():
    headers = {"X-API-Key": settings.DETECTOR_API_KEY}
    img_bytes = generate_test_image()

    response = client.post(
        "/v1/detect/image",
        headers=headers,
        files={"file": ("test.jpg", img_bytes, "image/jpeg")}
    )
    assert response.status_code == 200

    data = response.json()
    assert "image" in data
    assert "detections" in data
    assert len(data["detections"]) == 1
    assert data["detections"][0]["class"] == "D00"
    assert data["image"]["w"] == 640

def test_batch_detect():
    headers = {"X-API-Key": settings.DETECTOR_API_KEY}
    img_bytes = generate_test_image()

    response = client.post(
        "/v1/detect/batch",
        headers=headers,
        files=[
            ("files", ("test1.jpg", img_bytes, "image/jpeg")),
            ("files", ("test2.png", generate_test_image("PNG"), "image/png"))
        ]
    )
    assert response.status_code == 200

    data = response.json()
    assert len(data) == 2
    assert data[0]["filename"] == "test1.jpg"
    assert data[1]["filename"] == "test2.png"
    assert data[0]["detections"][0]["class"] == "D00"
