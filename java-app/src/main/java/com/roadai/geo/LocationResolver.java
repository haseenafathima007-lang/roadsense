package com.roadai.geo;

import com.roadai.domain.LocationFix;
import java.util.Optional;

public interface LocationResolver {
  Optional<LocationFix> resolve(LocationContext context);

  void setNext(LocationResolver next);
}
