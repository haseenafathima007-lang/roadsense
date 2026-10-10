package com.roadai.domain;

import com.roadai.config.Thresholds;

public class Pothole extends Defect {

  public Pothole(String id, LocationFix location) {
    super(id, location);
  }

  @Override
  public int escalation(Thresholds thresholds) {
    return thresholds.severityEscalationPothole();
  }

  @Override
  public DamageClass getDamageClass() {
    return DamageClass.POTHOLE;
  }
}
