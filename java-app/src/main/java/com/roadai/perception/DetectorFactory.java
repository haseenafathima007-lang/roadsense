package com.roadai.perception;

import java.time.Duration;

public class DetectorFactory {
  public static DamageDetector create(String endpointUrl, String apiKey) {
    if (endpointUrl == null || endpointUrl.isBlank()) {
      throw new IllegalArgumentException("Endpoint URL must be provided");
    }
    return new RemoteYoloDetector(endpointUrl, apiKey, Duration.ofSeconds(10));
  }
}
