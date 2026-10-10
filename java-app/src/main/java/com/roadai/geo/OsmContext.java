package com.roadai.geo;

import com.roadai.domain.GeoPoint;
import java.util.List;

/**
 * Read-only view of cached OSM road links.
 *
 * <p>Implementations MUST NOT call the network at runtime. All data is loaded once from a local
 * file at application start. ODbL attribution: © OpenStreetMap contributors.
 */
public interface OsmContext {

  /**
   * Return all road links whose geometry passes within {@code radiusM} of the given point. The
   * result is unordered and may over-include (implementations may use a bounding-box pre-filter).
   *
   * @param point query point
   * @param radiusM search radius in metres
   * @return candidate road links (may be empty, never null)
   */
  List<RoadLink> findNearbyLinks(GeoPoint point, double radiusM);
}
