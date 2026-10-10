package com.roadai.imaging;

import com.roadai.domain.Observation;
import java.util.List;

public interface QualityGate {
  GateResult check(ImageInfo info, List<Observation> observations);
}
