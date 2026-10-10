package com.roadai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.domain.GeoPoint;
import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import com.roadai.domain.Pothole;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LinkSummaryBuilderTest {

  private final LinkSummaryBuilder builder = new LinkSummaryBuilder();

  private LocationFix dummyLocation() {
    return new LocationFix(LocationSource.EXIF, new GeoPoint(13.0, 80.0), 5.0);
  }

  @Test
  @DisplayName("Groups defects by road link ID using Java streams groupingBy")
  void groupsDefectsByRoadLinkId() {
    Pothole d1 = new Pothole("d1", dummyLocation());
    d1.setRoadLinkId("osm-link-101");

    Pothole d2 = new Pothole("d2", dummyLocation());
    d2.setRoadLinkId("osm-link-101");

    Pothole d3 = new Pothole("d3", dummyLocation());
    d3.setRoadLinkId("osm-link-202");

    Pothole d4 = new Pothole("d4", dummyLocation()); // No road link assigned (null)

    Map<String, LinkSummary> summaries = builder.buildSummaries(List.of(d1, d2, d3, d4));

    assertThat(summaries).hasSize(2);
    assertThat(summaries).containsKey("osm-link-101");
    assertThat(summaries).containsKey("osm-link-202");

    LinkSummary link101 = summaries.get("osm-link-101");
    assertThat(link101.defectCount()).isEqualTo(2);
    assertThat(link101.defects()).containsExactlyInAnyOrder(d1, d2);

    LinkSummary link202 = summaries.get("osm-link-202");
    assertThat(link202.defectCount()).isEqualTo(1);
    assertThat(link202.defects()).containsExactly(d3);
  }
}
