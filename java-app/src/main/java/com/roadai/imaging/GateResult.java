package com.roadai.imaging;

import java.util.Collections;
import java.util.List;

public record GateResult(boolean pass, List<String> reasons) {
  public GateResult {
    if (reasons == null) {
      reasons = List.of();
    } else {
      reasons = List.copyOf(reasons);
    }
  }

  public static GateResult success() {
    return new GateResult(true, Collections.emptyList());
  }

  public static GateResult failure(String reason) {
    return new GateResult(false, List.of(reason));
  }
}
