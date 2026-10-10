package com.roadai.imaging;

import com.roadai.domain.Observation;
import java.util.List;

public class SizeGate implements QualityGate {
  private final int minWidth;
  private final int minHeight;

  public SizeGate(int minWidth, int minHeight) {
    this.minWidth = minWidth;
    this.minHeight = minHeight;
  }

  @Override
  public GateResult check(ImageInfo info, List<Observation> observations) {
    if (info.width() < minWidth || info.height() < minHeight) {
      return GateResult.failure(
          String.format(
              "Image resolution too low (%dx%d < %dx%d)",
              info.width(), info.height(), minWidth, minHeight));
    }
    return GateResult.success();
  }
}
