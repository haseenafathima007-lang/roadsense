package com.roadai.priority;

import com.roadai.domain.Defect;

/**
 * Placeholder exposure factor.
 *
 * <p>Returns {@code thresholds.priorityExposureDefault()} (currently 1.0 — neutral). This factor is
 * pluggable: when real traffic volume data becomes available, replace this implementation without
 * modifying {@link PriorityCalculator} or any other factor.
 *
 * <p>Value is UNVALIDATED starting assumption.
 */
public class ExposureFactor implements PriorityFactor {

  @Override
  public String name() {
    return "exposure";
  }

  @Override
  public double value(Defect defect, Context context) {
    return context.thresholds().priorityExposureDefault();
  }
}
