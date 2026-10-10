package com.roadai.geo;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.domain.Defect;
import com.roadai.domain.GeoPoint;
import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import com.roadai.domain.Pothole;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SpatialIndexTest {

  @Test
  @DisplayName("SpatialIndex results equal brute force on a randomised test (seeded 1337)")
  void spatialIndexMatchesBruteForceRandomized() {
    Random rng = new Random(1337);

    // Center around Chennai coordinates ~ 13.0827, 80.2707
    double baseLat = 13.0827;
    double baseLon = 80.2707;

    List<Defect> defects = new ArrayList<>();
    SpatialIndex spatialIndex = new SpatialIndex();

    // Generate 150 defects within ~1km box
    for (int i = 0; i < 150; i++) {
      // 0.005 deg ~ 550m
      double lat = baseLat + (rng.nextDouble() - 0.5) * 0.01;
      double lon = baseLon + (rng.nextDouble() - 0.5) * 0.01;
      LocationFix loc = new LocationFix(LocationSource.EXIF, new GeoPoint(lat, lon), 5.0);
      Defect d = new Pothole("defect-" + i, loc);
      defects.add(d);
      spatialIndex.add(d);
    }

    // Run 30 random queries with radii from 10m to 150m
    for (int q = 0; q < 30; q++) {
      double qLat = baseLat + (rng.nextDouble() - 0.5) * 0.01;
      double qLon = baseLon + (rng.nextDouble() - 0.5) * 0.01;
      GeoPoint center = new GeoPoint(qLat, qLon);
      double radiusM = 15.0 + rng.nextDouble() * 85.0; // 15m to 100m

      List<Defect> indexResults = spatialIndex.findNearby(center, radiusM);

      // Brute force calculation
      List<Defect> bruteForceResults =
          defects.stream()
              .filter(d -> Haversine.distanceM(center, d.getLocation().point()) <= radiusM)
              .toList();

      List<String> indexIds = indexResults.stream().map(Defect::getId).sorted().toList();
      List<String> bruteForceIds = bruteForceResults.stream().map(Defect::getId).sorted().toList();

      assertThat(indexIds)
          .as("SpatialIndex must match brute force for center %s and radius %.2fm", center, radiusM)
          .isEqualTo(bruteForceIds);
    }
  }
}
