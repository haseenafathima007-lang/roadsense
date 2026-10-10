package com.roadai.geo;

import com.roadai.domain.GeoPoint;
import java.util.List;

/**
 * A cached OSM road link: an ordered polyline of nodes representing one directed road segment.
 *
 * @param id OSM way ID (or internal surrogate)
 * @param highwayClass OSM highway tag value (e.g. "primary", "residential")
 * @param nodes ordered list of lat/lon points forming the polyline
 */
public record RoadLink(String id, String highwayClass, List<GeoPoint> nodes) {

  public RoadLink {
    if (nodes == null || nodes.size() < 2) {
      throw new IllegalArgumentException("RoadLink must have at least 2 nodes");
    }
    nodes = List.copyOf(nodes);
  }
}
