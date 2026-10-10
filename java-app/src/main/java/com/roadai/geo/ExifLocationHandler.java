package com.roadai.geo;

import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import java.util.Optional;

public class ExifLocationHandler extends AbstractLocationResolver {
  @Override
  public Optional<LocationFix> resolve(LocationContext context) {
    if (context.exif() != null && context.exif().gps().isPresent()) {
      return Optional.of(
          new LocationFix(
              LocationSource.EXIF, context.exif().gps().get(), 5.0)); // assume 5m accuracy for exif
    }
    return nextResolve(context);
  }
}
