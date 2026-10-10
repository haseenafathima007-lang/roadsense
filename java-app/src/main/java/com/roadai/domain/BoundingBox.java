package com.roadai.domain;

public record BoundingBox(int x1, int y1, int x2, int y2) {
  public BoundingBox {
    if (x1 < 0 || y1 < 0) {
      throw new IllegalArgumentException("Coordinates cannot be negative");
    }
    if (x1 >= x2 || y1 >= y2) {
      throw new IllegalArgumentException("Invalid bounding box coordinates");
    }
  }

  public int width() {
    return x2 - x1;
  }

  public int height() {
    return y2 - y1;
  }

  public int area() {
    return width() * height();
  }
}
