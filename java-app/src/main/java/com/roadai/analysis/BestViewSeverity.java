package com.roadai.analysis;

import com.roadai.config.Thresholds;
import com.roadai.domain.Defect;
import com.roadai.domain.Observation;
import com.roadai.domain.SeverityLevel;
import java.util.Objects;
import java.util.Optional;

public class BestViewSeverity implements SeverityStrategy {
  private final BestViewSelector selector;
  private final AreaRatioSeverity areaRatioSeverity;

  public BestViewSeverity(Thresholds thresholds) {
    this(new BestViewSelector(thresholds), new AreaRatioSeverity(thresholds));
  }

  public BestViewSeverity(BestViewSelector selector, AreaRatioSeverity areaRatioSeverity) {
    this.selector = Objects.requireNonNull(selector, "BestViewSelector cannot be null");
    this.areaRatioSeverity =
        Objects.requireNonNull(areaRatioSeverity, "AreaRatioSeverity cannot be null");
  }

  @Override
  public SeverityLevel calculateSeverity(Defect defect, Observation bestView) {
    Objects.requireNonNull(defect, "Defect cannot be null");

    Observation targetView = bestView;
    if (targetView == null) {
      Optional<BestViewResult> selection = selector.selectBest(defect.getObservations());
      if (selection.isEmpty()) {
        return SeverityLevel.LOW;
      }
      targetView = selection.get().observation();
    }

    SeverityLevel severity = areaRatioSeverity.calculateSeverity(defect, targetView);
    defect.setSeverity(severity);
    return severity;
  }

  public SeverityLevel calculateSeverity(Defect defect) {
    return calculateSeverity(defect, null);
  }
}
