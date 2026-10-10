package com.roadai.analysis;

import com.roadai.config.Thresholds;
import com.roadai.domain.Observation;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BestViewSelector {
  private final int edgeMarginPx;

  public BestViewSelector(Thresholds thresholds) {
    Objects.requireNonNull(thresholds, "Thresholds cannot be null");
    this.edgeMarginPx =
        thresholds.qualityEdgeTouchMarginPx() != null ? thresholds.qualityEdgeTouchMarginPx() : 0;
  }

  public BestViewSelector(int edgeMarginPx) {
    this.edgeMarginPx = edgeMarginPx;
  }

  public Optional<BestViewResult> selectBest(List<Observation> observations) {
    if (observations == null || observations.isEmpty()) {
      return Optional.empty();
    }

    Comparator<Observation> comparator =
        Comparator.comparingInt((Observation o) -> o.box().area())
            .thenComparing(Comparator.comparingInt((Observation o) -> o.box().y1()).reversed());

    List<Observation> nonTruncated = observations.stream().filter(this::isNonTruncated).toList();

    if (!nonTruncated.isEmpty()) {
      Observation best = nonTruncated.stream().max(comparator).orElseThrow();
      return Optional.of(new BestViewResult(best, false));
    }

    // All views are poor/truncated: fallback to best available, flagged as poor view
    Observation bestPoor = observations.stream().max(comparator).orElseThrow();
    return Optional.of(new BestViewResult(bestPoor, true));
  }

  public boolean isNonTruncated(Observation obs) {
    int x1 = obs.box().x1();
    int y1 = obs.box().y1();
    int x2 = obs.box().x2();
    int y2 = obs.box().y2();

    return x1 > edgeMarginPx
        && y1 > edgeMarginPx
        && x2 < obs.frameW() - edgeMarginPx
        && y2 < obs.frameH() - edgeMarginPx;
  }
}
