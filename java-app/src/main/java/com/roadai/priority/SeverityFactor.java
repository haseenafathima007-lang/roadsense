package com.roadai.priority;

import com.roadai.domain.Defect;
import com.roadai.domain.SeverityLevel;

/**
 * Converts the defect's {@link SeverityLevel} to a numeric multiplier.
 *
 * <ul>
 *   <li>LOW → 1.0
 *   <li>MEDIUM → 2.0
 *   <li>HIGH → 3.0
 *   <li>absent → 0.0 (defect ranked last)
 * </ul>
 */
public class SeverityFactor implements PriorityFactor {

  @Override
  public String name() {
    return "severity";
  }

  @Override
  public double value(Defect defect, Context context) {
    return defect
        .getSeverity()
        .map(
            s ->
                switch (s) {
                  case LOW -> 1.0;
                  case MEDIUM -> 2.0;
                  case HIGH -> 3.0;
                })
        .orElse(0.0);
  }
}
