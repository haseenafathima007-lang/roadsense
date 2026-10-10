package com.roadai.analysis;

import com.roadai.domain.DamageClass;
import com.roadai.domain.Observation;

public class CrackOrientationClassifier {

  /**
   * Hypothesis: Linear cracks can be roughly classified as longitudinal (vertical) or transverse
   * (horizontal) based on their bounding box aspect ratio. The detector scheme (RDD3) merges D00
   * and D10, so we use this geometric heuristic.
   */
  public CrackOrientation decide(Observation obs) {
    if (obs.damageClass() != DamageClass.CRACK_LINEAR) {
      throw new IllegalArgumentException("Observation must be of type CRACK_LINEAR");
    }

    double width = obs.box().width();
    double height = obs.box().height();

    if (height == 0) {
      return CrackOrientation.TRANSVERSE; // edge case
    }

    double aspectRatio = width / height;

    if (aspectRatio > 1.5) {
      return CrackOrientation.TRANSVERSE;
    } else if (aspectRatio < (1.0 / 1.5)) { // equivalent to height / width > 1.5
      return CrackOrientation.LONGITUDINAL;
    } else {
      // Ambiguous/fallback
      return CrackOrientation.LONGITUDINAL;
    }
  }
}
