package com.roadai.geo;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.domain.GeoPoint;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CachedOsmContextTest {

  private static final String SAMPLE_GEOJSON =
      """
      {
        "type": "FeatureCollection",
        "features": [
          {
            "type": "Feature",
            "properties": { "id": "way-1", "highway": "primary" },
            "geometry": {
              "type": "LineString",
              "coordinates": [[80.0, 13.0], [80.01, 13.0]]
            }
          },
          {
            "type": "Feature",
            "properties": { "id": "way-2", "highway": "residential" },
            "geometry": {
              "type": "LineString",
              "coordinates": [[80.5, 13.5], [80.51, 13.5]]
            }
          }
        ]
      }
      """;

  private CachedOsmContext load(String json) {
    InputStream is = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    return CachedOsmContext.load(is);
  }

  @Test
  @DisplayName("Loads GeoJSON and finds the nearby primary link")
  void loadsGeoJsonAndFindsLink() {
    CachedOsmContext ctx = load(SAMPLE_GEOJSON);
    assertThat(ctx.allLinks()).hasSize(2);

    // Query near way-1 (at 13.0, 80.005 — midpoint of way-1)
    List<RoadLink> nearby = ctx.findNearbyLinks(new GeoPoint(13.0, 80.005), 1000.0);
    assertThat(nearby).anyMatch(l -> "way-1".equals(l.id()));
  }

  @Test
  @DisplayName("Empty FeatureCollection returns no links")
  void emptyFeatureCollectionReturnsEmpty() {
    String emptyJson =
        """
        { "type": "FeatureCollection", "features": [] }
        """;
    CachedOsmContext ctx = load(emptyJson);
    assertThat(ctx.allLinks()).isEmpty();
    assertThat(ctx.findNearbyLinks(new GeoPoint(13, 80), 500)).isEmpty();
  }

  @Test
  @DisplayName("Non-LineString features are skipped silently")
  void nonLineStringFeaturesSkipped() {
    String json =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": { "id": "pt-1", "highway": "bus_stop" },
              "geometry": { "type": "Point", "coordinates": [80.0, 13.0] }
            }
          ]
        }
        """;
    CachedOsmContext ctx = load(json);
    assertThat(ctx.allLinks()).isEmpty();
  }

  @Test
  @DisplayName("Link far from query point is not returned")
  void farLinkNotReturned() {
    CachedOsmContext ctx = load(SAMPLE_GEOJSON);
    // way-2 is at ~13.5, 80.5 — query at 13.0, 80.005 with 100m radius
    List<RoadLink> nearby = ctx.findNearbyLinks(new GeoPoint(13.0, 80.005), 100.0);
    assertThat(nearby).noneMatch(l -> "way-2".equals(l.id()));
  }
}
