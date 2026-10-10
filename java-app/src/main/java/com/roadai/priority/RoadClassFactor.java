package com.roadai.priority;

import com.roadai.domain.Defect;
import com.roadai.geo.OsmContext;
import com.roadai.geo.RoadLink;
import com.roadai.geo.RoadLinkSnapper;
import java.util.List;
import java.util.Optional;

/**
 * Multiplies priority by the configured road class weight.
 *
 * <p>If the defect's road link id is already set (snapping done upstream), this factor looks up the
 * link from OsmContext to get the highway class. If no link is found or OSM data is absent, the
 * factor returns the neutral value 1.0.
 *
 * <p>Weights are UNVALIDATED starting assumptions from config.
 */
public class RoadClassFactor implements PriorityFactor {

  private final RoadLinkSnapper snapper;

  public RoadClassFactor(RoadLinkSnapper snapper) {
    this.snapper = snapper;
  }

  @Override
  public String name() {
    return "road_class";
  }

  @Override
  public double value(Defect defect, Context context) {
    if (defect.getLocation() == null) {
      return 1.0;
    }

    OsmContext osmContext = context.osmContext();
    double maxDist = context.thresholds().prioritySnapMaxDistanceM();
    List<RoadLink> nearby = osmContext.findNearbyLinks(defect.getLocation().point(), maxDist * 2);

    Optional<String> highwayClass =
        snapper
            .snap(defect.getLocation().point(), osmContext, maxDist)
            .map(r -> r.link().highwayClass());

    return highwayClass
        .map(cls -> context.thresholds().priorityRoadClassWeights().getOrDefault(cls, 1.0))
        .orElse(1.0);
  }
}
