package com.roadai.domain;

import com.roadai.config.Thresholds;

public class TransverseCrack extends Defect {

  public TransverseCrack(String id, LocationFix location) {
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
