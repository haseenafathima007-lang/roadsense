package com.roadai.analysis;

import com.roadai.domain.Observation;
import java.util.Objects;

public record BestViewResult(Observation observation, boolean isPoorView) {
  public BestViewResult {
    Objects.requireNonNull(observation, "Observation cannot be null");
  }
}
