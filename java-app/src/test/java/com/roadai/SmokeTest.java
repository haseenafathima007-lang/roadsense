package com.roadai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SmokeTest {

  @Test
  @DisplayName("Smoke test verifies testing framework and assertions function properly")
  void shouldPassSmokeTest() {
    assertThat(true).isTrue();
  }
}
