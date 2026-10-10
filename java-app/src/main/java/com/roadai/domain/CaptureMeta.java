package com.roadai.domain;

public record CaptureMeta(Double distanceBucketM, Double heightM, Double pitchDeg) {
  public CaptureMeta {
    if (distanceBucketM != null && distanceBucketM < 0) {
      throw new IllegalArgumentException("Distance cannot be negative");
    }
    if (heightM != null && heightM < 0) {
      throw new IllegalArgumentException("Height cannot be negative");
    }
    if (pitchDeg != null && (pitchDeg < -90 || pitchDeg > 90)) {
      throw new IllegalArgumentException("Pitch must be between -90 and 90 degrees");
    }
  }
}
