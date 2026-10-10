package com.roadai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.domain.BoundingBox;
import com.roadai.domain.DamageClass;
import com.roadai.domain.Observation;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BestViewSelectorTest {

  // Edge margin of 10px
  private final BestViewSelector selector = new BestViewSelector(10);

  @Test
  @DisplayName("Empty list returns Optional.empty()")
  void emptyListReturnsEmpty() {
    assertThat(selector.selectBest(List.of())).isEmpty();
  }

  @Test
  @DisplayName("Chooses largest non-truncated passing view among multiple views")
  void choosesLargestNonTruncatedView() {
    // Both are inside margin (10 to 630):
    // obs1: 100x100 = 10,000 area
    Observation obs1 =
        new Observation(
            "h1", DamageClass.POTHOLE, 0.8, new BoundingBox(100, 100, 200, 200), 640, 640);
    // obs2: 200x200 = 40,000 area
    Observation obs2 =
        new Observation(
            "h2", DamageClass.POTHOLE, 0.85, new BoundingBox(100, 100, 300, 300), 640, 640);

    Optional<BestViewResult> result = selector.selectBest(List.of(obs1, obs2));
    assertThat(result).isPresent();
    assertThat(result.get().observation()).isEqualTo(obs2);
    assertThat(result.get().isPoorView()).isFalse();
  }

  @Test
  @DisplayName("Prefers smaller non-truncated view over larger truncated view")
  void prefersSmallerNonTruncatedOverLargerTruncated() {
    // obsTruncated: large 500x500 = 250,000 area, but touches x1=5 <= 10 (truncated)
    Observation obsTruncated =
        new Observation("h1", DamageClass.POTHOLE, 0.9, new BoundingBox(5, 50, 505, 550), 640, 640);
    // obsGood: smaller 150x150 = 22,500 area, fully inside margin
    Observation obsGood =
        new Observation(
            "h2", DamageClass.POTHOLE, 0.8, new BoundingBox(100, 100, 250, 250), 640, 640);

    Optional<BestViewResult> result = selector.selectBest(List.of(obsTruncated, obsGood));
    assertThat(result).isPresent();
    assertThat(result.get().observation()).isEqualTo(obsGood);
    assertThat(result.get().isPoorView()).isFalse();
  }

  @Test
  @DisplayName("Falls back to best available if all are truncated/poor (and flags as poor view)")
  void fallsBackToBestAvailableWhenAllTruncated() {
    // obs1: truncated at x1=0, area 100x100 = 10,000
    Observation obs1 =
        new Observation(
            "h1", DamageClass.POTHOLE, 0.8, new BoundingBox(0, 100, 100, 200), 640, 640);
    // obs2: truncated at x2=640, area 200x200 = 40,000
    Observation obs2 =
        new Observation(
            "h2", DamageClass.POTHOLE, 0.8, new BoundingBox(440, 100, 640, 300), 640, 640);

    Optional<BestViewResult> result = selector.selectBest(List.of(obs1, obs2));
    assertThat(result).isPresent();
    assertThat(result.get().observation()).isEqualTo(obs2);
    assertThat(result.get().isPoorView()).isTrue();
  }
}
