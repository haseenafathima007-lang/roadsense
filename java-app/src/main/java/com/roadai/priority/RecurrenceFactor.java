package com.roadai.priority;

import com.roadai.domain.Defect;

/**
 * Increases priority for defects with repeated observations.
 *
 * <p>Formula: {@code 1.0 + (observationCount - 1) * recurrence_step}.
 *
 * <p>A single observation gives 1.0 (neutral). Each additional observation adds {@code
 * recurrence_step} (UNVALIDATED: 0.2). Example: 3 observations → 1.0 + 2 × 0.2 = 1.4.
 */
public class RecurrenceFactor implements PriorityFactor {

  @Override
  public String name() {
    return "recurrence";
  }

  @Override
  public double value(Defect defect, Context context) {
    int count = defect.getObservations().size();
    if (count <= 0) {
      return 1.0;
    }
    double step = context.thresholds().priorityRecurrenceStep();
    return 1.0 + (count - 1) * step;
  }
}
