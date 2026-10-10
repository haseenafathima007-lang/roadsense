package com.roadai.domain;

import com.roadai.config.Thresholds;

public class LongitudinalCrack extends Defect {

  public LongitudinalCrack(String id, LocationFix location) {
    super(id, location);
  }

  @Override
  public int escalation(Thresholds thresholds) {
    return thresholds.severityEscalationLinearCrack();
  }

  @Override
  public DamageClass getDamageClass() {
    return DamageClass.CRACK_LINEAR;
  }
}
