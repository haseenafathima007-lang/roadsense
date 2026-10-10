package com.roadai.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Guard test: severity thresholds are starting assumptions. Every severity key in thresholds.yaml
 * must keep the "UNVALIDATED" comment until they are measured and replaced with validated values
 * from the main document §5. This test fails if someone removes the comment prematurely.
 */
class UnvalidatedSeverityGuardTest {

  private static final Path THRESHOLDS_PATH = Path.of("../ai-service/app/config/thresholds.yaml");

  private static final List<String> SEVERITY_KEYS =
      List.of(
          "area_ratio_low_max",
          "area_ratio_medium_max",
          "escalation_pothole",
          "escalation_alligator",
          "escalation_linear_crack");

  @Test
  void allSeverityKeysRetainUnvalidatedComment() throws IOException {
    List<String> lines = Files.readAllLines(THRESHOLDS_PATH);

    for (String key : SEVERITY_KEYS) {
      boolean found = false;
      for (String line : lines) {
        if (line.contains(key + ":")) {
          found = true;
          assertThat(line)
              .as("Severity key '%s' must retain UNVALIDATED comment until measured", key)
              .containsIgnoringCase("UNVALIDATED");
          break;
        }
      }
      assertThat(found).as("Severity key '%s' must exist in thresholds.yaml", key).isTrue();
    }
  }
}
