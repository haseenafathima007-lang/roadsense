package com.roadai.analysis;

import com.roadai.domain.Defect;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LinkSummaryBuilder {

  public Map<String, LinkSummary> buildSummaries(List<Defect> defects) {
    return buildSummaries(defects, Defect::getRoadLinkId);
  }

  public Map<String, LinkSummary> buildSummaries(
      List<Defect> defects, Function<Defect, String> linkIdMapper) {
    Objects.requireNonNull(defects, "defects cannot be null");
    Objects.requireNonNull(linkIdMapper, "linkIdMapper cannot be null");

    return defects.stream()
        .filter(d -> linkIdMapper.apply(d) != null)
        .collect(
            Collectors.groupingBy(
                linkIdMapper,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    group -> new LinkSummary(linkIdMapper.apply(group.get(0)), group))));
  }
}
