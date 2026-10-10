package com.roadai.imaging;

import com.roadai.domain.Observation;
import java.util.List;

public class FramingGate implements QualityGate {
  private final int edgeMarginPx;

  public FramingGate(int edgeMarginPx) {
    this.edgeMarginPx = edgeMarginPx;
  }

  @Override
  public GateResult check(ImageInfo info, List<Observation> observations) {
    for (Observation obs : observations) {
      int x1 = obs.box().x1();
      int y1 = obs.box().y1();
      int x2 = obs.box().x2();
      int y2 = obs.box().y2();

      if (x1 <= edgeMarginPx
          || y1 <= edgeMarginPx
          || x2 >= info.width() - edgeMarginPx
          || y2 >= info.height() - edgeMarginPx) {
        return GateResult.failure("Detection touches image edge; likely truncated defect");
      }
    }
    return GateResult.success();
  }
}
