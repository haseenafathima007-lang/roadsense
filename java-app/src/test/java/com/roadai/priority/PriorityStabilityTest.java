package com.roadai.priority;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.config.Thresholds;
import com.roadai.domain.BoundingBox;
import com.roadai.domain.DamageClass;
import com.roadai.domain.GeoPoint;
import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import com.roadai.domain.Observation;
import com.roadai.domain.Pothole;
import com.roadai.domain.SeverityLevel;
import com.roadai.geo.OsmContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Priority stability test.
 *
 * <p>Generates 20 synthetic defects with randomised severity and observation counts (seeded for
 * determinism), ranks them, then perturbs all road-class weights by ±10% (20 trials, seeded) and
 * measures the fraction of the top-N that remains stable across perturbations.
 *
 * <p>The result is written to {@code docs/priority_stability.json} (never hand-typed).
 *
 * <p>Perturbation range: ±10% multiplicative on each road-class weight. Seed: 42.
 */
class PriorityStabilityTest {

  private static final int SEED = 42;
  private static final int NUM_DEFECTS = 20;
  private static final int PERTURBATION_TRIALS = 20;
  private static final double PERTURBATION_PCT = 0.10; // ±10%
  private static final Path OUTPUT_PATH = Path.of("../docs/priority_stability.json");

  private static final Map<String, Double> BASE_WEIGHTS =
      Map.of(
          "motorway",
          3.0,
          "trunk",
          2.5,
          "primary",
          2.0,
          "secondary",
          1.5,
          "tertiary",
          1.2,
          "residential",
          1.0);

  private static Thresholds makeThresholds(Map<String, Double> weights) {
    return new Thresholds(
        0.05, 0.15, 1, 1, 0, 15.0, 1.0, 40.0, 30.0, 0.70, 640, 480, 100.0, 40, 220, 10, 15.0, 0.65,
        0.25, 3, 20.0, 0.25, weights, 0.2, 0.2, 1.0, 50.0, 10);
  }

  private static LocationFix fix(int i) {
    return new LocationFix(LocationSource.EXIF, new GeoPoint(13.0 + i * 0.001, 80.0), 5.0);
  }

  @Test
  @DisplayName(
      "Stability test: seeded, ±10% weight perturbation, writes docs/priority_stability.json")
  void stabilityTestDeterministicSeed() throws IOException {
    Random rng = new Random(SEED);
    OsmContext emptyOsm = (p, r) -> List.of();

    // --- Build 20 synthetic defects ---
    SeverityLevel[] levels = SeverityLevel.values();
    List<Pothole> defects = new ArrayList<>();
    for (int i = 0; i < NUM_DEFECTS; i++) {
      Pothole p = new Pothole("d-" + i, fix(i));
      p.setSeverity(levels[rng.nextInt(levels.length)]);
      int obsCount = 1 + rng.nextInt(4);
      for (int j = 0; j < obsCount; j++) {
        p.addObservation(
            new Observation(
                "h-" + i + "-" + j,
                DamageClass.POTHOLE,
                0.9,
                new BoundingBox(10, 10, 50, 50),
                640,
                640));
      }
      defects.add(p);
    }

    // --- Baseline ranking ---
    PriorityCalculator baseCalc =
        new PriorityCalculator(
            List.of(new SeverityFactor(), new ExposureFactor(), new RecurrenceFactor()));
    Context baseCtx = new Context(makeThresholds(BASE_WEIGHTS), emptyOsm);
    List<com.roadai.domain.Defect> baseRanked =
        baseCalc.rankForMap(new ArrayList<>(defects), baseCtx);
    int topN = makeThresholds(BASE_WEIGHTS).priorityTopN();
    List<String> topNBaseline =
        baseRanked.stream().limit(topN).map(com.roadai.domain.Defect::getId).toList();

    // --- Perturbation trials ---
    int stableCount = 0;
    List<Integer> overlapCounts = new ArrayList<>();

    for (int trial = 0; trial < PERTURBATION_TRIALS; trial++) {
      Map<String, Double> perturbedWeights = new LinkedHashMap<>();
      for (Map.Entry<String, Double> e : BASE_WEIGHTS.entrySet()) {
        double factor = 1.0 + (rng.nextDouble() * 2 - 1) * PERTURBATION_PCT;
        perturbedWeights.put(e.getKey(), e.getValue() * factor);
      }
      Context pertCtx = new Context(makeThresholds(perturbedWeights), emptyOsm);
      List<com.roadai.domain.Defect> pertRanked =
          baseCalc.rankForMap(new ArrayList<>(defects), pertCtx);
      List<String> pertTopN =
          pertRanked.stream().limit(topN).map(com.roadai.domain.Defect::getId).toList();

      long overlap = pertTopN.stream().filter(topNBaseline::contains).count();
      overlapCounts.add((int) overlap);
      if (overlap >= topN) stableCount++;
    }

    double avgOverlap = overlapCounts.stream().mapToInt(i -> i).average().orElse(0);
    double stabilityRate = (double) stableCount / PERTURBATION_TRIALS;

    // --- Write JSON (hand-rolled, no extra dependency) ---
    String overlapJson =
        "[" + overlapCounts.stream().map(String::valueOf).collect(Collectors.joining(", ")) + "]";
    String baselineJson =
        "["
            + topNBaseline.stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", "))
            + "]";
    String json =
        String.format(
            """
            {
              "generated_at": "%s",
              "note": "SYNTHETIC DATA — generated by PriorityStabilityTest. NOT measured data.",
              "seed": %d,
              "num_defects": %d,
              "top_n": %d,
              "perturbation_trials": %d,
              "perturbation_pct": %.2f,
              "baseline_top_n": %s,
              "per_trial_overlap": %s,
              "avg_overlap_count": %.2f,
              "fully_stable_trials": %d,
              "stability_rate_pct": %.2f,
              "status": "NOT YET MEASURED on real data"
            }
            """,
            Instant.now(),
            SEED,
            NUM_DEFECTS,
            topN,
            PERTURBATION_TRIALS,
            PERTURBATION_PCT,
            baselineJson,
            overlapJson,
            avgOverlap,
            stableCount,
            stabilityRate * 100.0);

    Files.createDirectories(OUTPUT_PATH.getParent());
    Files.writeString(OUTPUT_PATH, json);

    // --- Assertions ---
    assertThat(OUTPUT_PATH).exists();
    // Avg overlap should be at least 7/10 (reasonable stability under ±10% perturbation)
    assertThat(avgOverlap).isGreaterThanOrEqualTo(7.0);
  }
}
