package com.roadai.geo;

import com.roadai.domain.GeoPoint;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * {@link OsmContext} backed by a local GeoJSON snapshot. Never calls the network at runtime.
 *
 * <p>Expected format: a GeoJSON {@code FeatureCollection} where each {@code Feature} has:
 *
 * <pre>
 * {
 *   "type": "Feature",
 *   "properties": { "id": "...", "highway": "primary" },
 *   "geometry": { "type": "LineString", "coordinates": [[lon, lat], ...] }
 * }
 * </pre>
 *
 * <p>ODbL attribution: data © OpenStreetMap contributors, licensed under the Open Database Licence.
 * See {@code data/osm/README.md} for the query metadata.
 */
public class CachedOsmContext implements OsmContext {

  private final List<RoadLink> links;
  // Coarse spatial index over link midpoints for bounding-box pre-filter
  private final SpatialIndex midpointIndex;

  private CachedOsmContext(List<RoadLink> links) {
    this.links = Collections.unmodifiableList(links);
    this.midpointIndex = new SpatialIndex();
    // We store dummy Defects keyed by index — instead, we hold a parallel index map
    // Actually: use a simple flat list scan for now (SpatialIndex is keyed to Defect).
    // The coarse pre-filter is done via bounding-box on lat/lon ranges.
  }

  /**
   * Load a GeoJSON FeatureCollection from the given stream. The stream is closed by the caller.
   *
   * @param jsonStream GeoJSON input stream
   * @return a ready-to-use {@link CachedOsmContext}
   */
  @SuppressWarnings("unchecked")
  public static CachedOsmContext load(InputStream jsonStream) {
    // Use SnakeYAML to parse JSON (it is a superset of JSON)
    Yaml yaml = new Yaml();
    Map<String, Object> root = yaml.load(jsonStream);

    List<RoadLink> result = new ArrayList<>();
    Object featuresObj = root.get("features");
    if (!(featuresObj instanceof List<?> features)) {
      return new CachedOsmContext(result);
    }

    for (Object featureObj : features) {
      if (!(featureObj instanceof Map<?, ?> feature)) continue;
      Map<String, Object> featureMap = (Map<String, Object>) feature;

      Map<String, Object> props = (Map<String, Object>) featureMap.get("properties");
      Map<String, Object> geom = (Map<String, Object>) featureMap.get("geometry");
      if (props == null || geom == null) continue;

      String id = String.valueOf(props.getOrDefault("id", "unknown"));
      String highway = String.valueOf(props.getOrDefault("highway", "unclassified"));
      String geomType = String.valueOf(geom.get("type"));
      if (!"LineString".equals(geomType)) continue;

      List<?> coords = (List<?>) geom.get("coordinates");
      if (coords == null || coords.size() < 2) continue;

      List<GeoPoint> nodes = new ArrayList<>();
      for (Object coordObj : coords) {
        List<?> coord = (List<?>) coordObj;
        double lon = ((Number) coord.get(0)).doubleValue();
        double lat = ((Number) coord.get(1)).doubleValue();
        nodes.add(new GeoPoint(lat, lon));
      }

      result.add(new RoadLink(id, highway, nodes));
    }

    return new CachedOsmContext(result);
  }

  @Override
  public List<RoadLink> findNearbyLinks(GeoPoint point, double radiusM) {
    // Flat-list scan with Haversine centroid pre-check.
    // Accurate enough for small OSM extracts; replace with spatial index when data grows.
    List<RoadLink> nearby = new ArrayList<>();
    for (RoadLink link : links) {
      GeoPoint centroid = midpoint(link);
      // Use 2× radius for centroid pre-filter to avoid missing long segments
      if (Haversine.distanceM(point, centroid) <= radiusM * 2.0) {
        nearby.add(link);
      }
    }
    return Collections.unmodifiableList(nearby);
  }

  /** Returns all loaded road links (for testing). */
  public List<RoadLink> allLinks() {
    return links;
  }

  private static GeoPoint midpoint(RoadLink link) {
    List<GeoPoint> nodes = link.nodes();
    double sumLat = 0, sumLon = 0;
    for (GeoPoint p : nodes) {
      sumLat += p.lat();
      sumLon += p.lon();
    }
    return new GeoPoint(sumLat / nodes.size(), sumLon / nodes.size());
  }
}
