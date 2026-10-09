# Contracts

## Python detection service
```
GET  /health                  -> {status, model, scheme}
POST /v1/detect/image         multipart: file
                              -> {image:{w,h}, detections:[{class, conf, bbox:[x1,y1,x2,y2]}]}
POST /v1/detect/batch         (optional) multipart: files[] -> [{filename, image, detections}]
```
- Auth header: `X-API-Key` (value from env `DETECTOR_API_KEY`). Change here first if you choose another name.
- Limits: allowed types image/jpeg, image/png; max size from env `MAX_UPLOAD_MB` (default 15).
- Error shape: `{"error": {"code": "<SNAKE_CASE>", "message": "<human text>"}}` with HTTP 400/401/413/415/500.
- `class` values depend on `scheme`: `rdd4` = D00,D10,D20,D40 ; `rdd3` = crack_linear, alligator, pothole (+ `patch` after Phase 2).
- bbox is in original-image pixel coordinates.

## Java `DamageDetector`
`List<Observation> detect(Path image) throws ApiException;` Stateless. Implementations: RemoteYoloDetector, FakeDetector, OnnxDetector (stretch).

## Thresholds
`ai-service/app/config/thresholds.yaml` is the single source (copied or read by Java `Thresholds`).
