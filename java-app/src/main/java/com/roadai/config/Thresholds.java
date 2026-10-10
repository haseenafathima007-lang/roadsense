package com.roadai.config;

import java.io.InputStream;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public record Thresholds(
    Double dedupMatchRadiusM,
    Double dedupAccuracyFactor,
    Double dedupMaxRadiusM,
    Double dedupManualPinRadiusM,
    Double dedupMinSimilarity,
    Integer qualityMinWidthPx,
    Integer qualityMinHeightPx,
    Double qualityMinBlurLaplacianVar,
    Integer qualityMinMeanLuma,
    Integer qualityMaxMeanLuma,
    Integer qualityEdgeTouchMarginPx,
    Double verifyVisitRadiusM,
    Double verifyMinSceneSimilarity,
    Double verifyClearConf,
    Integer surveyMinPhotosPerSegment,
    Double surveyTargetSpacingM,
    Double detectorMinConf) {

  public static Thresholds load(InputStream yamlStream) {
    Yaml yaml = new Yaml();
    Map<String, Object> root = yaml.load(yamlStream);

    return new Thresholds(
        getDouble(root, "dedup", "match_radius_m"),
        getDouble(root, "dedup", "accuracy_factor"),
        getDouble(root, "dedup", "max_radius_m"),
        getDouble(root, "dedup", "manual_pin_radius_m"),
        getDouble(root, "dedup", "min_similarity"),
        getInteger(root, "quality", "min_width_px"),
        getInteger(root, "quality", "min_height_px"),
        getDouble(root, "quality", "min_blur_laplacian_var"),
        getInteger(root, "quality", "min_mean_luma"),
        getInteger(root, "quality", "max_mean_luma"),
        getInteger(root, "quality", "edge_touch_margin_px"),
        getDouble(root, "verify", "visit_radius_m"),
        getDouble(root, "verify", "min_scene_similarity"),
        getDouble(root, "verify", "clear_conf"),
        getInteger(root, "survey", "min_photos_per_segment"),
        getDouble(root, "survey", "target_spacing_m"),
        getDouble(root, "detector", "min_conf"));
  }

  private static Double getDouble(Map<String, Object> root, String section, String key) {
    Object val = getRaw(root, section, key);
    if (val instanceof Number n) {
      return n.doubleValue();
    }
    throw new IllegalStateException(
        String.format("Expected double for %s.%s but got %s", section, key, val));
  }

  private static Integer getInteger(Map<String, Object> root, String section, String key) {
    Object val = getRaw(root, section, key);
    if (val instanceof Number n) {
      return n.intValue();
    }
    throw new IllegalStateException(
        String.format("Expected integer for %s.%s but got %s", section, key, val));
  }

  @SuppressWarnings("unchecked")
  private static Object getRaw(Map<String, Object> root, String section, String key) {
    if (!root.containsKey(section)) {
      throw new IllegalStateException("Missing section: " + section);
    }
    Map<String, Object> secMap = (Map<String, Object>) root.get(section);
    if (!secMap.containsKey(key)) {
      throw new IllegalStateException("Missing key: " + section + "." + key);
    }
    Object val = secMap.get(key);
    if (val == null) {
      throw new IllegalStateException("Value is null for key: " + section + "." + key);
    }
    return val;
  }
}
