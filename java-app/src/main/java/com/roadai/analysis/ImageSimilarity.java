package com.roadai.analysis;

import java.nio.file.Path;

public interface ImageSimilarity {
  /**
   * Calculates visual similarity between two images.
   *
   * @param imageA Path to first image
   * @param imageB Path to second image
   * @return Score between 0.0 (completely different) and 1.0 (identical)
   */
  double score(Path imageA, Path imageB);
}
