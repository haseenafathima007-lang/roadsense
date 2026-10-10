package com.roadai.priority;

import com.roadai.domain.Defect;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Computes priority scores and ranks defects.
 *
 * <p>The factor list is provided at construction time. Adding a new {@link PriorityFactor} requires
 * only a new class implementing the interface and registering it here — no existing class changes.
 *
 * <p>Formula: score = product of all factor values (clamped to ≥ 0).
 */
public class PriorityCalculator {

  private final List<PriorityFactor> factors;

  public PriorityCalculator(List<PriorityFactor> factors) {
    this.factors = List.copyOf(Objects.requireNonNull(factors, "factors cannot be null"));
  }

  /**
   * Calculate the priority result for a single defect.
   *
   * @param defect the defect to score
   * @param context runtime context
   * @return a {@link PriorityResult} with score and per-factor breakdown
   */
  public PriorityResult calculate(Defect defect, Context context) {
    Objects.requireNonNull(defect, "defect cannot be null");
    Objects.requireNonNull(context, "context cannot be null");

    List<FactorContribution> contributions = new ArrayList<>();
    double score = 1.0;

    for (PriorityFactor factor : factors) {
      double raw = factor.value(defect, context);
      double clamped = Math.max(0.0, raw);
      contributions.add(new FactorContribution(factor.name(), clamped));
      score *= clamped;
    }

    return new PriorityResult(score, List.copyOf(contributions));
  }

  /**
   * Rank defects that have a location, ordered by descending priority score. Defects with no
   * location fix are excluded — they belong in the {@link #locationNeeded} list.
   *
   * @param defects all defects to consider
   * @param context runtime context
   * @return ranked list (highest score first), location-less defects excluded
   */
  public List<Defect> rankForMap(List<Defect> defects, Context context) {
    Objects.requireNonNull(defects, "defects cannot be null");
    return defects.stream()
        .filter(d -> d.getLocation() != null)
        .sorted(Comparator.comparingDouble((Defect d) -> calculate(d, context).score()).reversed())
        .toList();
  }

  /**
   * Returns defects that have no location fix and therefore cannot appear on the ranked map.
   *
   * @param defects all defects
   * @return list of defects with null location (LOCATION_NEEDED)
   */
  public List<Defect> locationNeeded(List<Defect> defects) {
    Objects.requireNonNull(defects, "defects cannot be null");
    return defects.stream().filter(d -> d.getLocation() == null).toList();
  }
}
