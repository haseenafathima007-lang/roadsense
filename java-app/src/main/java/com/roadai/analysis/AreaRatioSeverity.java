package com.roadai.analysis;

import com.roadai.config.Thresholds;
import com.roadai.domain.Defect;
import com.roadai.domain.Observation;
import com.roadai.domain.SeverityLevel;
import java.util.Objects;

public class AreaRatioSeverity implements SeverityStrategy {
  private final Thresholds thresholds;

  public AreaRatioSeverity(Thresholds thresholds) {
    this.thresholds = Objects.requireNonNull(thresholds, "Thresholds cannot be null");
  }

  @Override
  public SeverityLevel calculateSeverity(Defect defect, Observation bestView) {
    Objects.requireNonNull(defect, "Defect cannot be null");
    Objects.requireNonNull(bestView, "Observation cannot be null");

    double boxArea = bestView.box().area();
    double frameArea = (double) bestView.frameW() * bestView.frameH();
    double ratio = boxArea / frameArea;

    SeverityLevel baseLevel;
    if (ratio <= thresholds.severityAreaRatioLowMax()) {
      baseLevel = SeverityLevel.LOW;
    } else if (ratio <= thresholds.severityAreaRatioMediumMax()) {
      baseLevel = SeverityLevel.MEDIUM;
    } else {
      baseLevel = SeverityLevel.HIGH;
    }

    int steps = defect.escalation(thresholds);
    return escalate(baseLevel, steps);
  }

  private SeverityLevel escalate(SeverityLevel level, int steps) {
    if (steps <= 0) {
      return level;
    }
    int newOrdinal = Math.min(SeverityLevel.HIGH.ordinal(), level.ordinal() + steps);
    return SeverityLevel.values()[newOrdinal];
  }
}
