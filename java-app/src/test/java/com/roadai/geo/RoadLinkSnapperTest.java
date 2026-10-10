package com.roadai.geo;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.domain.GeoPoint;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Synthetic road-network fixture: a crossroads at (13.0, 80.0) with:
 *
 * <pre>
 *   Road A (N-S): (12.999, 80.0) -- (13.001, 80.0)   highway=primary
 *   Road B (E-W): (13.0, 79.999) -- (13.0, 80.001)   highway=secondary
 *   Road C (parallel to A): (12.999, 80.01) -- (13.001, 80.01) highway=residential
 * </pre>
 */
class RoadLinkSnapperTest {

  private static final double MAX_DIST_M = 100.0;

  private RoadLinkSnapper snapper;
  private RoadLink roadA; // N-S primary
  private RoadLink roadB; // E-W secondary
  private RoadLink roadC; // parallel residential

  @BeforeEach
  void setUp() {
    snapper = new RoadLinkSnapper();
    roadA =
        new RoadLink(
            "A", "primary", List.of(new GeoPoint(12.999, 80.0), new GeoPoint(13.001, 80.0)));
    roadB =
        new RoadLink(
            "B", "secondary", List.of(new GeoPoint(13.0, 79.999), new GeoPoint(13.0, 80.001)));
    roadC =
        new RoadLink(
            "C", "residential", List.of(new GeoPoint(12.999, 80.01), new GeoPoint(13.001, 80.01)));
  }

  private OsmContext contextWith(RoadLink... links) {
    List<RoadLink> linkList = List.of(links);
    return (point, radius) -> linkList;
  }

  @Test
  @DisplayName("Crossroads: point directly north of junction snaps to N-S road (A)")
  void snapsToNearestSegmentCrossroads() {
    // Point is 50m north of the junction, on the N-S road line
    GeoPoint query = new GeoPoint(13.00045, 80.0); // north along road A
    OsmContext ctx = contextWith(roadA, roadB);

    Optional<SnapResult> result = snapper.snap(query, ctx, MAX_DIST_M);

    assertThat(result).isPresent();
    assertThat(result.get().link().id()).isEqualTo("A");
    assertThat(result.get().distanceM()).isLessThan(5.0); // very close to N-S road
  }

  @Test
  @DisplayName("Parallel roads: point halfway between A and C snaps to whichever is closer")
  void snapsToNearestSegmentParallelRoads() {
    // Road C is at lon=80.01, Road A at lon=80.0
    // Point at lon=80.0003 is closer to Road A
    GeoPoint query = new GeoPoint(13.0, 80.0003);
    OsmContext ctx = contextWith(roadA, roadC);

    Optional<SnapResult> result = snapper.snap(query, ctx, MAX_DIST_M);

    assertThat(result).isPresent();
    assertThat(result.get().link().id()).isEqualTo("A");
  }

  @Test
  @DisplayName("Far point > maxDist returns empty (not an error)")
  void farPointExceedsMaxDistance() {
    // Point is ~3 km from road A
    GeoPoint query = new GeoPoint(13.03, 80.0);
    OsmContext ctx = contextWith(roadA, roadB, roadC);

    Optional<SnapResult> result = snapper.snap(query, ctx, 50.0); // 50m max

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Empty OSM context returns empty")
  void emptyOsmContextReturnsEmpty() {
    OsmContext emptyCtx = (p, r) -> List.of();
    Optional<SnapResult> result = snapper.snap(new GeoPoint(13.0, 80.0), emptyCtx, MAX_DIST_M);
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("SnapResult contains projected point on the segment (not outside endpoints)")
  void snapResultProjectedPointIsOnSegment() {
    // Point west of the start of road A
    GeoPoint query = new GeoPoint(12.9985, 80.0);
    OsmContext ctx = contextWith(roadA);

    Optional<SnapResult> result = snapper.snap(query, ctx, MAX_DIST_M);
    assertThat(result).isPresent();
    GeoPoint proj = result.get().projectionPoint();
    // Projected lat must be clamped to [12.999, 13.001]
    assertThat(proj.lat()).isGreaterThanOrEqualTo(12.999);
    assertThat(proj.lat()).isLessThanOrEqualTo(13.001);
  }
}
