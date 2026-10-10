package com.roadai.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.roadai.domain.BoundingBox;
import com.roadai.domain.DamageClass;
import com.roadai.domain.Observation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CrackOrientationClassifierTest {

  private final CrackOrientationClassifier classifier = new CrackOrientationClassifier();

  @Test
  @DisplayName("Wide bounding box classifies as TRANSVERSE (D10)")
  void wideBoxIsTransverse() {
    Observation obs =
        new Observation(
            "hash1", DamageClass.CRACK_LINEAR, 0.9, new BoundingBox(10, 10, 100, 30), 640, 640);
    assertThat(classifier.decide(obs)).isEqualTo(CrackOrientation.TRANSVERSE);
  }

  @Test
  @DisplayName("Tall bounding box classifies as LONGITUDINAL (D00)")
  void tallBoxIsLongitudinal() {
    Observation obs =
        new Observation(
            "hash2", DamageClass.CRACK_LINEAR, 0.9, new BoundingBox(10, 10, 30, 100), 640, 640);
    assertThat(classifier.decide(obs)).isEqualTo(CrackOrientation.LONGITUDINAL);
  }

  @Test
  @DisplayName("Roughly square box falls back to LONGITUDINAL default")
  void squareBoxFallback() {
    Observation obs =
        new Observation(
            "hash3", DamageClass.CRACK_LINEAR, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640);
    assertThat(classifier.decide(obs)).isEqualTo(CrackOrientation.LONGITUDINAL);
  }

  @Test
  @DisplayName("Non-crack observation throws IllegalArgumentException")
  void nonCrackThrows() {
    Observation obs =
        new Observation(
            "hash4", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640);
    assertThatThrownBy(() -> classifier.decide(obs)).isInstanceOf(IllegalArgumentException.class);
  }
}
