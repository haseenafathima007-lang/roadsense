package com.roadai.domain;

import com.roadai.config.Thresholds;

public class AlligatorCrack extends Defect {

  public AlligatorCrack(String id, LocationFix location) {
    super(id, location);
  }

  @Override
  public int escalation(Thresholds thresholds) {
    return thresholds.severityEscalationAlligator();
  }

  @Override
  public DamageClass getDamageClass() {
    return DamageClass.ALLIGATOR;
  }
}
