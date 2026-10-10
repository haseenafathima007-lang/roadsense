package com.roadai.geo;

import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import java.util.Optional;

public class DeviceLocationHandler extends AbstractLocationResolver {
  @Override
  public Optional<LocationFix> resolve(LocationContext context) {
    if (context.deviceLocation() != null) {
      return Optional.of(new LocationFix(LocationSource.DEVICE, context.deviceLocation(), 10.0));
    }
    return nextResolve(context);
  }
}
