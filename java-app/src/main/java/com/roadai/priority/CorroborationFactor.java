package com.roadai.priority;

import com.roadai.domain.Defect;
import com.roadai.domain.Observation;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Optional factor: increases priority when multiple independent reporters have photographed the
 * same defect.
 *
 * <p>Formula: {@code 1.0 + (distinctReporterCount - 1) * corroboration_step}.
 *
 * <p>A single reporter gives 1.0 (neutral). "Reporter" is taken as the {@code imageHash} field of
 * each {@link Observation} — a distinct hash proxy for a distinct photo from a distinct person.
 * When a real {@code reporterId} field is available, this class should be updated; no other class
 * needs changing.
 *
 * <p>Demonstrates the open/closed extension point: this class was registered in the {@link
 * PriorityCalculator} constructor without editing any existing class.
 */
public class CorroborationFactor implements PriorityFactor {

  @Override
  public String name() {
    return "corroboration";
  }

  @Override
  public double value(Defect defect, Context context) {
    Set<String> distinctHashes =
        defect.getObservations().stream().map(Observation::fileHash).collect(Collectors.toSet());
    int count = distinctHashes.size();
    if (count <= 1) {
      return 1.0;
    }
    double step = context.thresholds().priorityCorroborationStep();
    return 1.0 + (count - 1) * step;
  }
}
