package com.roadai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ThresholdsTest {

  @Test
  void loadsValidYaml() {
    String yaml =
        """
            severity:
              area_ratio_low_max: 0.05
              area_ratio_medium_max: 0.15
              escalation_pothole: 1
              escalation_alligator: 1
              escalation_linear_crack: 0
            dedup:
              match_radius_m: 15
              accuracy_factor: 1.0
              max_radius_m: 40
              manual_pin_radius_m: 30
              min_similarity: 0.60
            quality:
              min_width_px: 640
              min_height_px: 480
              min_blur_laplacian_var: 100.0
              min_mean_luma: 40
              max_mean_luma: 225
              edge_touch_margin_px: 4
            verify:
              visit_radius_m: 25
              min_scene_similarity: 0.55
              clear_conf: 0.25
            survey:
              min_photos_per_segment: 3
              target_spacing_m: 10
            detector:
              min_conf: 0.25
            """;

    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));
    Thresholds thresholds = Thresholds.load(is);

    assertThat(thresholds.severityAreaRatioLowMax()).isEqualTo(0.05);
    assertThat(thresholds.severityAreaRatioMediumMax()).isEqualTo(0.15);
    assertThat(thresholds.severityEscalationPothole()).isEqualTo(1);
    assertThat(thresholds.severityEscalationAlligator()).isEqualTo(1);
    assertThat(thresholds.severityEscalationLinearCrack()).isEqualTo(0);
    assertThat(thresholds.dedupMatchRadiusM()).isEqualTo(15.0);
    assertThat(thresholds.qualityMinWidthPx()).isEqualTo(640);
    assertThat(thresholds.detectorMinConf()).isEqualTo(0.25);
  }

  @Test
  void throwsOnMissingSection() {
    String yaml = "detector:\n  min_conf: 0.25\n";
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> Thresholds.load(is))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing section: severity");
  }

  @Test
  void throwsOnMissingKey() {
    String yaml =
        """
            severity:
              area_ratio_low_max: 0.05
            """; // Missing other severity keys
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> Thresholds.load(is))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Missing key: severity.area_ratio_medium_max");
  }

  @Test
  void throwsOnNullValue() {
    String yaml =
        """
            severity:
              area_ratio_low_max: null
            """;
    InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

    assertThatThrownBy(() -> Thresholds.load(is))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Value is null for key: severity.area_ratio_low_max");
  }
}
