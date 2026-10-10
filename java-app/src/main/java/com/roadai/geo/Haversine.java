package com.roadai.geo;

import com.roadai.domain.GeoPoint;

public class Haversine {
  private static final double R_EARTH_M = 6371000.0;

  public static double distanceM(GeoPoint p1, GeoPoint p2) {
    double dLat = Math.toRadians(p2.lat() - p1.lat());
    double dLon = Math.toRadians(p2.lon() - p1.lon());

    double a =
        Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(p1.lat()))
                * Math.cos(Math.toRadians(p2.lat()))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R_EARTH_M * c;
  }
}
