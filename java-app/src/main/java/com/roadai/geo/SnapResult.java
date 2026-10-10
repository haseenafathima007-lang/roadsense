package com.roadai.geo;

import com.roadai.domain.GeoPoint;

/**
 * The result of snapping a point to a road link.
 *
 * @param link the matched road link
 * @param distanceM distance in metres from the query point to the projected point on the segment
 * @param projectionPoint the nearest point on the segment to the query point
 */
public record SnapResult(RoadLink link, double distanceM, GeoPoint projectionPoint) {}
