package com.roadai.config;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public record Thresholds(
    Double severityAreaRatioLowMax,
    Double severityAreaRatioMediumMax,
    Integer severityEscalationPothole,
    Integer severityEscalationAlligator,
    Integer severityEscalationLinearCrack,
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
    Double detectorMinConf,
    // Priority weights — UNVALIDATED starting assumptions
    Map<String, Double> priorityRoadClassWeights,
    Double priorityRecurrenceStep,
    Double priorityCorroborationStep,
    Double priorityExposureDefault,
    Double prioritySnapMaxDistanceM,
    Integer priorityTopN) {

  public static Thresholds load(InputStream yamlStream) {
    Yaml yaml = new Yaml();
    Map<String, Object> root = yaml.load(yamlStream);

    return new Thresholds(
        getDouble(root, "severity", "area_ratio_low_max"),
        getDouble(root, "severity", "area_ratio_medium_max"),
        getInteger(root, "severity", "escalation_pothole"),
        getInteger(root, "severity", "escalation_alligator"),
        getInteger(root, "severity", "escalation_linear_crack"),
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
        getDouble(root, "detector", "min_conf"),
        getRoadClassWeights(root),
        getDouble(root, "priority", "recurrence_step"),
        getDouble(root, "priority", "corroboration_step"),
        getDouble(root, "priority", "exposure_default"),
        getDouble(root, "priority", "snap_max_distance_m"),
        getInteger(root, "priority", "top_n_stability"));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Double> getRoadClassWeights(Map<String, Object> root) {
    Object section = root.get("priority");
    if (!(section instanceof Map<?, ?> secMap)) {
      throw new IllegalStateException("Missing section: priority");
    }
    Object weights = ((Map<String, Object>) secMap).get("road_class_weight");
    if (!(weights instanceof Map<?, ?> wMap)) {
      throw new IllegalStateException("Missing key: priority.road_class_weight");
    }
    Map<String, Object> rawWeights = (Map<String, Object>) wMap;
    Map<String, Double> result = new LinkedHashMap<>();
    for (Map.Entry<String, Object> e : rawWeights.entrySet()) {
      if (e.getValue() instanceof Number n) {
        result.put(e.getKey(), n.doubleValue());
      } else {
        throw new IllegalStateException(
            "priority.road_class_weight." + e.getKey() + " is null or non-numeric");
      }
    }
    return Collections.unmodifiableMap(result);
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
