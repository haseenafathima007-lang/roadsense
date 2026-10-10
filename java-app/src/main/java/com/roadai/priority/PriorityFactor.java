package com.roadai.priority;

import com.roadai.domain.Defect;

/**
 * A single composable factor in the priority formula.
 *
 * <p>Priority = product of all factor values. Adding a new factor requires only registering it in
 * the {@link PriorityCalculator} constructor — no existing class needs to change.
 *
 * <p>Implementors must return a value ≥ 0. Zero means the defect is ranked last.
 */
public interface PriorityFactor {

  /** Human-readable name used in {@link FactorContribution} for explainability. */
  String name();

  /**
   * Compute this factor's contribution for the given defect.
   *
   * @param defect the defect being scored
   * @param context runtime context (thresholds + OSM)
   * @return a non-negative multiplier; returning 0 sets the whole product to 0
   */
  double value(Defect defect, Context context);
}
