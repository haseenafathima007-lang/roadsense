package com.roadai.geo;

import com.roadai.domain.GeoPoint;
import com.roadai.imaging.ExifResult;

public record LocationContext(ExifResult exif, GeoPoint deviceLocation, GeoPoint manualPin) {
  public LocationContext {
    // Can be null
  }
}
