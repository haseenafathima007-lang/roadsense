package com.roadai.imaging;

import com.roadai.domain.GeoPoint;
import java.time.Instant;
import java.util.Optional;

public record ExifResult(
    Optional<Instant> captureTime,
    Optional<GeoPoint> gps,
    Optional<String> cameraMake,
    Optional<String> cameraModel) {}
