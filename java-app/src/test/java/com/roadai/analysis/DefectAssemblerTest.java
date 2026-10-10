package com.roadai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.config.Thresholds;
import com.roadai.domain.BoundingBox;
import com.roadai.domain.CaptureMeta;
import com.roadai.domain.DamageClass;
import com.roadai.domain.Defect;
import com.roadai.domain.GeoPoint;
import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import com.roadai.domain.LongitudinalCrack;
import com.roadai.domain.Observation;
import com.roadai.domain.Pothole;
import com.roadai.domain.Report;
import com.roadai.lifecycle.MachineState;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefectAssemblerTest {

  @TempDir Path tempDir;

  private Thresholds thresholds;
  private CrackOrientationClassifier orientationClassifier;
  private Map<String, Path> imageMap;

  @BeforeEach
  void setUp() {
    thresholds =
        new Thresholds(
            0.05, 0.15, 1, 1, 0, 15.0, // match_radius_m
            1.0, // accuracy_factor
            40.0, // max_radius_m
            30.0, // manual_pin_radius_m
            0.70, // min_similarity
            640, 480, 100.0, 40, 220, 10, 15.0, 0.65, 0.25, 3, 20.0, 0.25);
    orientationClassifier = new CrackOrientationClassifier();
    imageMap = new HashMap<>();
  }

  private Path createDummyImageFile(String name) throws IOException {
    Path p = tempDir.resolve(name + ".jpg");
    Files.write(p, new byte[] {1, 2, 3});
    imageMap.put(name, p);
    return p;
  }

  private GeoPoint pointAtMetersNorth(GeoPoint base, double metersNorth) {
    double dLat = (metersNorth / 6371000.0) * (180.0 / Math.PI);
    return new GeoPoint(base.lat() + dLat, base.lon());
  }

  private Report createReport(
      String fileHash, GeoPoint point, Double accuracyM, LocationSource source, Observation obs) {
    LocationFix loc = point != null ? new LocationFix(source, point, accuracyM) : null;
    return new Report(
        fileHash, Instant.now(), loc, new CaptureMeta(null, null, null), List.of(obs));
  }

  @Test
  @DisplayName("Branch 1: Different damage families do NOT merge (Pothole vs Linear Crack)")
  void branch1_differentDamageFamiliesDoNotMerge() throws IOException {
    GeoPoint origin = new GeoPoint(13.0, 80.0);
    createDummyImageFile("img1");
    createDummyImageFile("img2");

    Pothole existing = new Pothole("p1", new LocationFix(LocationSource.EXIF, origin, 5.0));
    existing.addObservation(
        new Observation(
            "img1", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));

    // Incoming report at the exact same location but is CRACK_LINEAR
    Observation crackObs =
        new Observation(
            "img2", DamageClass.CRACK_LINEAR, 0.9, new BoundingBox(10, 10, 100, 20), 640, 640);
    Report incomingReport = createReport("img2", origin, 5.0, LocationSource.EXIF, crackObs);

    ImageSimilarity sim = (a, b) -> 0.95; // high similarity
    DefectAssembler assembler =
        new DefectAssembler(
            thresholds,
            sim,
            orientationClassifier,
            hash -> Optional.ofNullable(imageMap.get(hash)));

    AssemblerResult result = assembler.assemble(incomingReport, List.of(existing));

    assertThat(result.getMergedDefects()).isEmpty();
    assertThat(result.getNewDefects()).hasSize(1);
    assertThat(result.getNewDefects().get(0).getDamageClass()).isEqualTo(DamageClass.CRACK_LINEAR);
  }

  @Test
  @DisplayName("Branch 2: Distance exceeds allowable radius -> do NOT merge")
  void branch2_distanceExceedsAllowableRadiusDoNotMerge() throws IOException {
    GeoPoint origin = new GeoPoint(13.0, 80.0);
    createDummyImageFile("img1");
    createDummyImageFile("img2");

    Pothole existing = new Pothole("p1", new LocationFix(LocationSource.EXIF, origin, 0.0));
    existing.addObservation(
        new Observation(
            "img1", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));

    // Base match radius is 15m (accuracy = 0). Incoming is 16m away -> outside
    GeoPoint outside = pointAtMetersNorth(origin, 16.0);
    Observation potholeObs =
        new Observation(
            "img2", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640);
    Report incomingReport = createReport("img2", outside, 0.0, LocationSource.EXIF, potholeObs);

    ImageSimilarity sim = (a, b) -> 0.95;
    DefectAssembler assembler =
        new DefectAssembler(
            thresholds,
            sim,
            orientationClassifier,
            hash -> Optional.ofNullable(imageMap.get(hash)));

    AssemblerResult result = assembler.assemble(incomingReport, List.of(existing));

    assertThat(result.getMergedDefects()).isEmpty();
    assertThat(result.getNewDefects()).hasSize(1);
    assertThat(result.getNewDefects().get(0).getObservations()).containsExactly(potholeObs);
  }

  @Test
  @DisplayName("Branch 3: Within allowable radius AND similarity >= minSimilarity -> MERGE")
  void branch3_sameLocationAndHighSimilarityMerges() throws IOException {
    GeoPoint origin = new GeoPoint(13.0, 80.0);
    createDummyImageFile("img1");
    createDummyImageFile("img2");

    Pothole existing = new Pothole("p1", new LocationFix(LocationSource.EXIF, origin, 5.0));
    Observation obs1 =
        new Observation(
            "img1", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640);
    existing.addObservation(obs1);

    // Incoming at 5m away (< 15m), similarity = 0.85 (>= 0.70)
    GeoPoint nearby = pointAtMetersNorth(origin, 5.0);
    Observation obs2 =
        new Observation(
            "img2", DamageClass.POTHOLE, 0.85, new BoundingBox(15, 15, 55, 55), 640, 640);
    Report incomingReport = createReport("img2", nearby, 5.0, LocationSource.EXIF, obs2);

    ImageSimilarity sim = (a, b) -> 0.85;
    DefectAssembler assembler =
        new DefectAssembler(
            thresholds,
            sim,
            orientationClassifier,
            hash -> Optional.ofNullable(imageMap.get(hash)));

    AssemblerResult result = assembler.assemble(incomingReport, List.of(existing));

    assertThat(result.getMergedDefects()).containsExactly(existing);
    assertThat(result.getNewDefects()).isEmpty();
    assertThat(existing.getObservations()).containsExactly(obs1, obs2);
  }

  @Test
  @DisplayName(
      "Branch 4: Within allowable radius BUT similarity < minSimilarity -> do NOT merge (creates new defect)")
  void branch4_sameLocationAndLowSimilarityCreatesDistinctDefect() throws IOException {
    GeoPoint origin = new GeoPoint(13.0, 80.0);
    createDummyImageFile("img1");
    createDummyImageFile("img2");

    Pothole existing = new Pothole("p1", new LocationFix(LocationSource.EXIF, origin, 5.0));
    existing.addObservation(
        new Observation(
            "img1", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));

    // Incoming at 5m away, but similarity = 0.40 (< 0.70) -> distinct defect
    GeoPoint nearby = pointAtMetersNorth(origin, 5.0);
    Observation obs2 =
        new Observation(
            "img2", DamageClass.POTHOLE, 0.85, new BoundingBox(15, 15, 55, 55), 640, 640);
    Report incomingReport = createReport("img2", nearby, 5.0, LocationSource.EXIF, obs2);

    ImageSimilarity sim = (a, b) -> 0.40;
    DefectAssembler assembler =
        new DefectAssembler(
            thresholds,
            sim,
            orientationClassifier,
            hash -> Optional.ofNullable(imageMap.get(hash)));

    AssemblerResult result = assembler.assemble(incomingReport, List.of(existing));

    assertThat(result.getMergedDefects()).isEmpty();
    assertThat(result.getNewDefects()).hasSize(1);
    assertThat(result.getNewDefects().get(0).getMachineState()).isEqualTo(MachineState.NEW);
  }

  @Test
  @DisplayName(
      "Branch 5: Doubt case (similarity unavailable) -> NEEDS_REVIEW, not a merge and not a split")
  void branch5_similarityUnavailableCreatesNeedsReviewLinkNotMergeNotSplit() {
    GeoPoint origin = new GeoPoint(13.0, 80.0);
    // Images are not registered in imageMap -> similarity unavailable

    Pothole existing = new Pothole("p1", new LocationFix(LocationSource.EXIF, origin, 5.0));
    existing.addObservation(
        new Observation(
            "missing-img1", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));

    GeoPoint nearby = pointAtMetersNorth(origin, 5.0);
    Observation incomingObs =
        new Observation(
            "missing-img2", DamageClass.POTHOLE, 0.85, new BoundingBox(10, 10, 50, 50), 640, 640);
    Report incomingReport =
        createReport("missing-img2", nearby, 5.0, LocationSource.EXIF, incomingObs);

    ImageSimilarity sim = (a, b) -> 0.95;
    DefectAssembler assembler =
        new DefectAssembler(
            thresholds,
            sim,
            orientationClassifier,
            hash -> Optional.ofNullable(imageMap.get(hash)));

    AssemblerResult result = assembler.assemble(incomingReport, List.of(existing));

    // Must be NOT a merge and NOT a split!
    assertThat(result.getMergedDefects()).as("Doubt case must not be an automatic merge").isEmpty();
    assertThat(result.getNewDefects())
        .as("Doubt case must not be a clean new defect split")
        .isEmpty();
    assertThat(result.getReviewLinks()).as("Doubt case must create a ReviewLink").hasSize(1);

    ReviewLink link = result.getReviewLinks().get(0);
    assertThat(link.existingDefect()).isEqualTo(existing);
    assertThat(link.newDefect().getMachineState()).isEqualTo(MachineState.NEEDS_REVIEW);
    assertThat(result.getNeedsReviewDefects()).hasSize(1);
  }

  @Test
  @DisplayName("Accuracy widening boundary test: inside, on, and outside boundary distance")
  void accuracyWideningBoundaryTests() throws IOException {
    // base match_radius = 15m, accuracy_factor = 1.0, accuracy = 10m -> allowable = 25m
    GeoPoint origin = new GeoPoint(13.0, 80.0);
    createDummyImageFile("img-base");
    createDummyImageFile("img-inside");
    createDummyImageFile("img-on");
    createDummyImageFile("img-outside");

    ImageSimilarity highSim = (a, b) -> 0.90;
    DefectAssembler assembler =
        new DefectAssembler(
            thresholds,
            highSim,
            orientationClassifier,
            hash -> Optional.ofNullable(imageMap.get(hash)));

    // 1. Inside boundary: 24.0m (< 25m) -> MERGE
    Pothole p1 = new Pothole("p1", new LocationFix(LocationSource.EXIF, origin, 10.0));
    p1.addObservation(
        new Observation(
            "img-base", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    Report insideReport =
        createReport(
            "img-inside",
            pointAtMetersNorth(origin, 24.0),
            10.0,
            LocationSource.EXIF,
            new Observation(
                "img-inside", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    AssemblerResult resInside = assembler.assemble(insideReport, List.of(p1));
    assertThat(resInside.getMergedDefects()).hasSize(1);

    // 2. On boundary: 25.0m (<= 25m) -> MERGE
    Pothole p2 = new Pothole("p2", new LocationFix(LocationSource.EXIF, origin, 10.0));
    p2.addObservation(
        new Observation(
            "img-base", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    Report onReport =
        createReport(
            "img-on",
            pointAtMetersNorth(origin, 25.0),
            10.0,
            LocationSource.EXIF,
            new Observation(
                "img-on", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    AssemblerResult resOn = assembler.assemble(onReport, List.of(p2));
    assertThat(resOn.getMergedDefects()).hasSize(1);

    // 3. Outside boundary: 25.5m (> 25m) -> DO NOT MERGE
    Pothole p3 = new Pothole("p3", new LocationFix(LocationSource.EXIF, origin, 10.0));
    p3.addObservation(
        new Observation(
            "img-base", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    Report outsideReport =
        createReport(
            "img-outside",
            pointAtMetersNorth(origin, 25.5),
            10.0,
            LocationSource.EXIF,
            new Observation(
                "img-outside",
                DamageClass.POTHOLE,
                0.9,
                new BoundingBox(10, 10, 50, 50),
                640,
                640));
    AssemblerResult resOutside = assembler.assemble(outsideReport, List.of(p3));
    assertThat(resOutside.getMergedDefects()).isEmpty();
    assertThat(resOutside.getNewDefects()).hasSize(1);
  }

  @Test
  @DisplayName("Accuracy widening cap: large accuracy (50m) capped at max_radius_m (40m)")
  void accuracyWideningCappedAtMaxRadius() throws IOException {
    // 15 + 1.0 * 50 = 65m, capped at max_radius_m = 40m
    GeoPoint origin = new GeoPoint(13.0, 80.0);
    createDummyImageFile("img-base");
    createDummyImageFile("img-inside");
    createDummyImageFile("img-outside");

    ImageSimilarity highSim = (a, b) -> 0.90;
    DefectAssembler assembler =
        new DefectAssembler(
            thresholds,
            highSim,
            orientationClassifier,
            hash -> Optional.ofNullable(imageMap.get(hash)));

    // 1. Inside cap: 39.0m (< 40m) -> MERGE
    Pothole p1 = new Pothole("p1", new LocationFix(LocationSource.EXIF, origin, 50.0));
    p1.addObservation(
        new Observation(
            "img-base", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    Report insideCapReport =
        createReport(
            "img-inside",
            pointAtMetersNorth(origin, 39.0),
            50.0,
            LocationSource.EXIF,
            new Observation(
                "img-inside", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    AssemblerResult resInside = assembler.assemble(insideCapReport, List.of(p1));
    assertThat(resInside.getMergedDefects()).hasSize(1);

    // 2. Outside cap: 41.0m (> 40m) -> DO NOT MERGE (even though 41 < 65)
    Pothole p2 = new Pothole("p2", new LocationFix(LocationSource.EXIF, origin, 50.0));
    p2.addObservation(
        new Observation(
            "img-base", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    Report outsideCapReport =
        createReport(
            "img-outside",
            pointAtMetersNorth(origin, 41.0),
            50.0,
            LocationSource.EXIF,
            new Observation(
                "img-outside",
                DamageClass.POTHOLE,
                0.9,
                new BoundingBox(10, 10, 50, 50),
                640,
                640));
    AssemblerResult resOutside = assembler.assemble(outsideCapReport, List.of(p2));
    assertThat(resOutside.getMergedDefects()).isEmpty();
    assertThat(resOutside.getNewDefects()).hasSize(1);
  }

  @Test
  @DisplayName("Manual pin uses dedicated manual pin radius (30m)")
  void manualPinUsesDedicatedRadius() throws IOException {
    GeoPoint origin = new GeoPoint(13.0, 80.0);
    createDummyImageFile("img-base");
    createDummyImageFile("img-pin-in");
    createDummyImageFile("img-pin-out");

    ImageSimilarity highSim = (a, b) -> 0.90;
    DefectAssembler assembler =
        new DefectAssembler(
            thresholds,
            highSim,
            orientationClassifier,
            hash -> Optional.ofNullable(imageMap.get(hash)));

    // Manual pin at 28m (< 30m) -> MERGE
    Pothole p1 = new Pothole("p1", new LocationFix(LocationSource.EXIF, origin, 5.0));
    p1.addObservation(
        new Observation(
            "img-base", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    Report pinInReport =
        createReport(
            "img-pin-in",
            pointAtMetersNorth(origin, 28.0),
            null,
            LocationSource.MANUAL_PIN,
            new Observation(
                "img-pin-in", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    AssemblerResult resIn = assembler.assemble(pinInReport, List.of(p1));
    assertThat(resIn.getMergedDefects()).hasSize(1);

    // Manual pin at 32m (> 30m) -> DO NOT MERGE
    Pothole p2 = new Pothole("p2", new LocationFix(LocationSource.EXIF, origin, 5.0));
    p2.addObservation(
        new Observation(
            "img-base", DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640));
    Report pinOutReport =
        createReport(
            "img-pin-out",
            pointAtMetersNorth(origin, 32.0),
            null,
            LocationSource.MANUAL_PIN,
            new Observation(
                "img-pin-out",
                DamageClass.POTHOLE,
                0.9,
                new BoundingBox(10, 10, 50, 50),
                640,
                640));
    AssemblerResult resOut = assembler.assemble(pinOutReport, List.of(p2));
    assertThat(resOut.getMergedDefects()).isEmpty();
    assertThat(resOut.getNewDefects()).hasSize(1);
  }

  @Test
  @DisplayName("Same crack family (D00 Longitudinal & D10 Transverse) matches family and merges")
  void sameCrackFamilyD00AndD10Merges() throws IOException {
    GeoPoint origin = new GeoPoint(13.0, 80.0);
    createDummyImageFile("img1");
    createDummyImageFile("img2");

    // Existing is LongitudinalCrack (vertical box 20x100)
    Defect existing =
        new LongitudinalCrack("c1", new LocationFix(LocationSource.EXIF, origin, 5.0));
    existing.addObservation(
        new Observation(
            "img1", DamageClass.CRACK_LINEAR, 0.9, new BoundingBox(10, 10, 30, 110), 640, 640));

    // Incoming is Transverse Crack (horizontal box 100x20) at same location, high similarity
    Observation incomingObs =
        new Observation(
            "img2", DamageClass.CRACK_LINEAR, 0.9, new BoundingBox(10, 10, 110, 30), 640, 640);
    Report incomingReport = createReport("img2", origin, 5.0, LocationSource.EXIF, incomingObs);

    ImageSimilarity highSim = (a, b) -> 0.85;
    DefectAssembler assembler =
        new DefectAssembler(
            thresholds,
            highSim,
            orientationClassifier,
            hash -> Optional.ofNullable(imageMap.get(hash)));

    AssemblerResult result = assembler.assemble(incomingReport, List.of(existing));

    assertThat(result.getMergedDefects()).containsExactly(existing);
    assertThat(result.getNewDefects()).isEmpty();
  }
}
