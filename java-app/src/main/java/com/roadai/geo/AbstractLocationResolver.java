package com.roadai.geo;

import com.roadai.domain.LocationFix;
import java.util.Optional;

public abstract class AbstractLocationResolver implements LocationResolver {
  private LocationResolver next;

  @Override
  public void setNext(LocationResolver next) {
    this.next = next;
  }

  protected Optional<LocationFix> nextResolve(LocationContext context) {
    if (next != null) {
      return next.resolve(context);
    }
    return Optional.empty();
  }
}
