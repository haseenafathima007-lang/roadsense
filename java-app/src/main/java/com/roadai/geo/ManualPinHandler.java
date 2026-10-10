package com.roadai.geo;

import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import java.util.Optional;

public class ManualPinHandler extends AbstractLocationResolver {
  @Override
  public Optional<LocationFix> resolve(LocationContext context) {
    if (context.manualPin() != null) {
      return Optional.of(new LocationFix(LocationSource.MANUAL_PIN, context.manualPin(), null));
    }
    return nextResolve(context);
  }
}
