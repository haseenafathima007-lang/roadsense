package com.roadai.geo;

import com.roadai.domain.Defect;
import com.roadai.domain.GeoPoint;
import com.roadai.domain.LocationFix;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpatialIndex {

  // About 111km per degree. 0.001 degree ~ 111m.
  // For radii around 15-40m, 0.001 degree grid size is good enough
  // to keep buckets small but avoid missing neighbors (max search in adjacent buckets).
  private static final double GRID_SIZE_DEG = 0.001;

  private final Map<GridCell, List<Defect>> index = new HashMap<>();

  public void add(Defect defect) {
    LocationFix loc = defect.getLocation();
    if (loc != null && loc.point() != null) {
      GridCell cell = toGridCell(loc.point());
      index.computeIfAbsent(cell, k -> new ArrayList<>()).add(defect);
    }
  }

  public void addAll(Collection<Defect> defects) {
    for (Defect d : defects) {
      add(d);
    }
  }

  public List<Defect> findNearby(GeoPoint center, double maxRadiusM) {
    List<Defect> candidates = new ArrayList<>();
    GridCell centerCell = toGridCell(center);

    // Calculate how many cells we need to search in each direction.
    // Roughly 111,000 meters per degree latitude.
    // maxRadiusM / 111,000 gives degrees, then / GRID_SIZE_DEG gives cells.
    int cellsToSearchLat = (int) Math.ceil((maxRadiusM / 111000.0) / GRID_SIZE_DEG);
    // Longitude varies, 111000 * cos(lat)
    double cosLat = Math.cos(Math.toRadians(center.lat()));
    double mPerDegLon = 111000.0 * Math.max(cosLat, 0.1); // avoid /0 at poles
    int cellsToSearchLon = (int) Math.ceil((maxRadiusM / mPerDegLon) / GRID_SIZE_DEG);

    for (int i = -cellsToSearchLat; i <= cellsToSearchLat; i++) {
      for (int j = -cellsToSearchLon; j <= cellsToSearchLon; j++) {
        GridCell cell = new GridCell(centerCell.latIndex + i, centerCell.lonIndex + j);
        List<Defect> defectsInCell = index.get(cell);
        if (defectsInCell != null) {
          for (Defect d : defectsInCell) {
            double dist = Haversine.distanceM(center, d.getLocation().point());
            if (dist <= maxRadiusM + 1e-6) {
              candidates.add(d);
            }
          }
        }
      }
    }

    return candidates;
  }

  private GridCell toGridCell(GeoPoint point) {
    int latIndex = (int) Math.floor(point.lat() / GRID_SIZE_DEG);
    int lonIndex = (int) Math.floor(point.lon() / GRID_SIZE_DEG);
    return new GridCell(latIndex, lonIndex);
  }

  private record GridCell(int latIndex, int lonIndex) {}
}
