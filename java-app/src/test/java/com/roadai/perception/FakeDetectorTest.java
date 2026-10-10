package com.roadai.perception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.roadai.domain.BoundingBox;
import com.roadai.domain.DamageClass;
import com.roadai.domain.Observation;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class FakeDetectorTest {

  @Test
  void returnsScriptedOutputs() throws ApiException {
    Observation obs =
        new Observation(DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 100, 100);
    FakeDetector detector = new FakeDetector(List.of(obs));

    List<Observation> results = detector.detect(Path.of("dummy.jpg"));

    assertThat(results).hasSize(1);
    assertThat(results.get(0).damageClass()).isEqualTo(DamageClass.POTHOLE);
  }

  @Test
  void canSimulateFailure() {
    FakeDetector detector = new FakeDetector(List.of());
    detector.setShouldFail(true);

    assertThatThrownBy(() -> detector.detect(Path.of("dummy.jpg")))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Simulated failure")
        .extracting("statusCode")
        .isEqualTo(500);
  }
}
