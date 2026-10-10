package com.roadai.analysis;

import com.roadai.domain.BoundingBox;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class UnionAreaCalculator {

  public long calculateUnionArea(List<BoundingBox> boxes) {
    if (boxes == null || boxes.isEmpty()) {
      return 0L;
    }

    // Coordinate compression along the X axis
    TreeSet<Integer> xSet = new TreeSet<>();
    for (BoundingBox b : boxes) {
      xSet.add(b.x1());
      xSet.add(b.x2());
    }

    List<Integer> xCoords = new ArrayList<>(xSet);
    long totalUnionArea = 0L;

    for (int i = 0; i < xCoords.size() - 1; i++) {
      int xLeft = xCoords.get(i);
      int xRight = xCoords.get(i + 1);
      int dx = xRight - xLeft;

      if (dx <= 0) {
        continue;
      }

      // Collect Y-intervals of boxes covering [xLeft, xRight]
      List<YInterval> yIntervals = new ArrayList<>();
      for (BoundingBox b : boxes) {
        if (b.x1() <= xLeft && b.x2() >= xRight) {
          yIntervals.add(new YInterval(b.y1(), b.y2()));
        }
      }

      if (yIntervals.isEmpty()) {
        continue;
      }

      // Merge overlapping Y intervals
      yIntervals.sort(
          Comparator.comparingInt((YInterval iv) -> iv.y1).thenComparingInt(iv -> iv.y2));

      long totalY = 0L;
      int curY1 = yIntervals.get(0).y1;
      int curY2 = yIntervals.get(0).y2;

      for (int j = 1; j < yIntervals.size(); j++) {
        YInterval iv = yIntervals.get(j);
        if (iv.y1 <= curY2) {
          curY2 = Math.max(curY2, iv.y2);
        } else {
          totalY += (curY2 - curY1);
          curY1 = iv.y1;
          curY2 = iv.y2;
        }
      }
      totalY += (curY2 - curY1);

      totalUnionArea += (long) dx * totalY;
    }

    return totalUnionArea;
  }

  private record YInterval(int y1, int y2) {}
}
