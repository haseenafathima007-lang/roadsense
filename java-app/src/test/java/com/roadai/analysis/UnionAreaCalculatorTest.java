package com.roadai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.domain.BoundingBox;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnionAreaCalculatorTest {

  private final UnionAreaCalculator calculator = new UnionAreaCalculator();

  @Test
  @DisplayName("Empty or null list returns 0")
  void emptyReturnsZero() {
    assertThat(calculator.calculateUnionArea(null)).isEqualTo(0L);
    assertThat(calculator.calculateUnionArea(List.of())).isEqualTo(0L);
  }

  @Test
  @DisplayName("Single box returns its own area")
  void singleBoxReturnsArea() {
    BoundingBox b = new BoundingBox(10, 20, 50, 70); // 40 x 50 = 2000
    assertThat(calculator.calculateUnionArea(List.of(b))).isEqualTo(2000L);
  }

  @Test
  @DisplayName("Identical overlapping boxes return single box area")
  void identicalBoxesReturnSingleArea() {
    BoundingBox b1 = new BoundingBox(10, 10, 60, 60); // 50 x 50 = 2500
    BoundingBox b2 = new BoundingBox(10, 10, 60, 60);
    assertThat(calculator.calculateUnionArea(List.of(b1, b2))).isEqualTo(2500L);
  }

  @Test
  @DisplayName("Completely disjoint boxes return sum of areas")
  void disjointBoxesReturnSum() {
    BoundingBox b1 = new BoundingBox(0, 0, 10, 10); // 100
    BoundingBox b2 = new BoundingBox(20, 20, 30, 30); // 100
    assertThat(calculator.calculateUnionArea(List.of(b1, b2))).isEqualTo(200L);
  }

  @Test
  @DisplayName("Partially overlapping boxes match hand arithmetic: 100 + 100 - 25 = 175")
  void overlappingBoxesMatchHandArithmetic() {
    // Box A: (0, 0) to (10, 10), area = 100
    BoundingBox b1 = new BoundingBox(0, 0, 10, 10);
    // Box B: (5, 5) to (15, 15), area = 100
    BoundingBox b2 = new BoundingBox(5, 5, 15, 15);
    // Intersection: (5, 5) to (10, 10), area = 25
    // Union: 100 + 100 - 25 = 175
    assertThat(calculator.calculateUnionArea(List.of(b1, b2))).isEqualTo(175L);
  }

  @Test
  @DisplayName("Property tests: union <= sum, union >= max, order independent on randomized boxes")
  void propertyTestsOnRandomBoxes() {
    Random rng = new Random(42); // Deterministic seed

    for (int trial = 0; trial < 20; trial++) {
      int count = 5 + rng.nextInt(10);
      List<BoundingBox> boxes = new ArrayList<>();
      long sumAreas = 0;
      long maxArea = 0;

      for (int i = 0; i < count; i++) {
        int x1 = rng.nextInt(500);
        int y1 = rng.nextInt(500);
        int w = 10 + rng.nextInt(100);
        int h = 10 + rng.nextInt(100);
        BoundingBox b = new BoundingBox(x1, y1, x1 + w, y1 + h);
        boxes.add(b);
        sumAreas += b.area();
        maxArea = Math.max(maxArea, b.area());
      }

      long union = calculator.calculateUnionArea(boxes);

      // Property 1: union <= sum of individual areas
      assertThat(union)
          .as("Union area must be <= sum of individual areas")
          .isLessThanOrEqualTo(sumAreas);

      // Property 2: union >= max individual area
      assertThat(union).as("Union area must be >= max single area").isGreaterThanOrEqualTo(maxArea);

      // Property 3: order independence
      List<BoundingBox> shuffled = new ArrayList<>(boxes);
      Collections.reverse(shuffled);
      long reversedUnion = calculator.calculateUnionArea(shuffled);
      assertThat(reversedUnion)
          .as("Union area must be identical regardless of box order")
          .isEqualTo(union);
    }
  }
}
