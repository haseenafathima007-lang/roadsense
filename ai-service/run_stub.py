import uvicorn
from app.main import app
from app.detector import StubDetector, get_detector

app.dependency_overrides[get_detector] = lambda: StubDetector()

if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)
