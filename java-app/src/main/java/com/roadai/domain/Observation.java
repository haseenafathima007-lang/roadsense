package com.roadai.domain;

import java.util.Objects;

public record Observation(
    DamageClass damageClass, double confidence, BoundingBox box, int frameW, int frameH) {
  public Observation {
    Objects.requireNonNull(damageClass, "Damage class cannot be null");
    Objects.requireNonNull(box, "Bounding box cannot be null");
    if (confidence < 0.0 || confidence > 1.0) {
      throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
    }
    if (frameW <= 0 || frameH <= 0) {
      throw new IllegalArgumentException("Frame dimensions must be positive");
    }
    if (box.x2() > frameW || box.y2() > frameH) {
      throw new IllegalArgumentException("Bounding box exceeds frame dimensions");
    }
  }
}
