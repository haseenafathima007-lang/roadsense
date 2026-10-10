package com.roadai.analysis;

import com.roadai.domain.Defect;
import com.roadai.domain.Observation;
import com.roadai.domain.SeverityLevel;

public interface SeverityStrategy {
  SeverityLevel calculateSeverity(Defect defect, Observation bestView);
}
