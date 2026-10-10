package com.roadai.domain;

import java.util.Objects;

public record LocationFix(LocationSource source, GeoPoint point, Double accuracyM) {
  public LocationFix {
    Objects.requireNonNull(source, "Location source cannot be null");
    Objects.requireNonNull(point, "GeoPoint cannot be null");
    if (accuracyM != null && accuracyM < 0) {
      throw new IllegalArgumentException("Accuracy cannot be negative");
    }
  }
}
