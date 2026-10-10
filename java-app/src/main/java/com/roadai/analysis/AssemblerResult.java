package com.roadai.analysis;

import com.roadai.domain.Defect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AssemblerResult {
  private final List<Defect> newDefects = new ArrayList<>();
  private final List<Defect> mergedDefects = new ArrayList<>();
  private final List<ReviewLink> reviewLinks = new ArrayList<>();

  public void addNewDefect(Defect defect) {
    newDefects.add(Objects.requireNonNull(defect, "defect cannot be null"));
  }

  public void addMerged(Defect defect) {
    mergedDefects.add(Objects.requireNonNull(defect, "defect cannot be null"));
  }

  public void addReviewLink(ReviewLink link) {
    reviewLinks.add(Objects.requireNonNull(link, "link cannot be null"));
  }

  public List<Defect> getNewDefects() {
    return Collections.unmodifiableList(newDefects);
  }

  public List<Defect> getMergedDefects() {
    return Collections.unmodifiableList(mergedDefects);
  }

  public List<ReviewLink> getReviewLinks() {
    return Collections.unmodifiableList(reviewLinks);
  }

  public List<Defect> getNeedsReviewDefects() {
    return reviewLinks.stream().map(ReviewLink::newDefect).toList();
  }
}
