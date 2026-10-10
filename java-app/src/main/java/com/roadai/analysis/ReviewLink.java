package com.roadai.analysis;

import com.roadai.domain.Defect;
import java.util.Objects;

public record ReviewLink(Defect newDefect, Defect existingDefect, String reason) {
  public ReviewLink {
    Objects.requireNonNull(newDefect, "newDefect cannot be null");
    Objects.requireNonNull(existingDefect, "existingDefect cannot be null");
    Objects.requireNonNull(reason, "reason cannot be null");
  }
}
