package com.roadai.geo;

import com.roadai.domain.GeoPoint;
import java.util.List;
import java.util.Optional;

/**
 * Snaps a query point to the nearest road link segment.
 *
 * <h2>Approximation</h2>
 *
 * Point-to-segment distance uses a <em>local equirectangular</em> projection:
 *
 * <pre>
 *   dx = (lon2 - lon1) * cos(midLat * PI/180) * 111_320
 *   dy = (lat2 - lat1) * 111_320
 * </pre>
 *
 * where {@code midLat} is the mean latitude of the two segment endpoints. This introduces an error
 * of ≈ 0.1 % within 1 km of the reference latitude — acceptable for snapping purposes. No claims of
 * exact metre-level precision are made.
 *
 * <p>If the nearest link is farther than {@code maxDistM}, {@link Optional#empty()} is returned
 * (not an error).
 */
public class RoadLinkSnapper {

  private static final double METRES_PER_DEGREE_LAT = 111_320.0;

  /**
   * Snap {@code point} to the nearest segment across all nearby road links.
   *
   * @param point query point
   * @param osmContext OSM data source
   * @param maxDistM maximum allowable snap distance in metres
   * @return snap result, or empty if nothing is within {@code maxDistM}
   */
  public Optional<SnapResult> snap(GeoPoint point, OsmContext osmContext, double maxDistM) {
    List<RoadLink> candidates = osmContext.findNearbyLinks(point, maxDistM);

    SnapResult best = null;
    double bestDist = Double.MAX_VALUE;

    for (RoadLink link : candidates) {
      List<GeoPoint> nodes = link.nodes();
      for (int i = 0; i < nodes.size() - 1; i++) {
        GeoPoint a = nodes.get(i);
        GeoPoint b = nodes.get(i + 1);
        ProjectionResult proj = projectPointToSegment(point, a, b);
        if (proj.distM() < bestDist) {
          bestDist = proj.distM();
          best = new SnapResult(link, proj.distM(), proj.projected());
        }
      }
    }

    if (best == null || bestDist > maxDistM) {
      return Optional.empty();
    }
    return Optional.of(best);
  }

  /**
   * Project {@code p} onto segment [{@code a}, {@code b}] using local equirectangular
   * approximation.
   */
  private static ProjectionResult projectPointToSegment(GeoPoint p, GeoPoint a, GeoPoint b) {
    double midLat = (a.lat() + b.lat()) / 2.0;
    double cosLat = Math.cos(Math.toRadians(midLat));

    // Convert to local Cartesian (metres)
    double ax = a.lon() * cosLat * METRES_PER_DEGREE_LAT;
    double ay = a.lat() * METRES_PER_DEGREE_LAT;
    double bx = b.lon() * cosLat * METRES_PER_DEGREE_LAT;
    double by = b.lat() * METRES_PER_DEGREE_LAT;
    double px = p.lon() * cosLat * METRES_PER_DEGREE_LAT;
    double py = p.lat() * METRES_PER_DEGREE_LAT;

    double dx = bx - ax;
    double dy = by - ay;
    double lenSq = dx * dx + dy * dy;

    double projLat, projLon;
    if (lenSq < 1e-10) {
      // Degenerate segment — snap to endpoint a
      projLat = a.lat();
      projLon = a.lon();
    } else {
      double t = Math.clamp(((px - ax) * dx + (py - ay) * dy) / lenSq, 0.0, 1.0);
      projLat = a.lat() + t * (b.lat() - a.lat());
      projLon = a.lon() + t * (b.lon() - a.lon());
    }

    GeoPoint projected = new GeoPoint(projLat, projLon);
    double distM = Haversine.distanceM(p, projected);
    return new ProjectionResult(projected, distM);
  }

  private record ProjectionResult(GeoPoint projected, double distM) {}
}
