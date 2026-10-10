package com.roadai.analysis;

import com.roadai.domain.Defect;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record LinkSummary(String linkId, List<Defect> defects, int defectCount) {
  public LinkSummary(String linkId, List<Defect> defects) {
    this(
        Objects.requireNonNull(linkId, "linkId cannot be null"),
        Collections.unmodifiableList(List.copyOf(defects)),
        defects.size());
  }
}
