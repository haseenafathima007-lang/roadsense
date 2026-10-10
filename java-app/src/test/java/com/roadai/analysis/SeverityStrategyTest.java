package com.roadai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.config.Thresholds;
import com.roadai.domain.AlligatorCrack;
import com.roadai.domain.BoundingBox;
import com.roadai.domain.DamageClass;
import com.roadai.domain.Defect;
import com.roadai.domain.GeoPoint;
import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import com.roadai.domain.LongitudinalCrack;
import com.roadai.domain.Observation;
import com.roadai.domain.Pothole;
import com.roadai.domain.SeverityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SeverityStrategyTest {

  private Thresholds thresholds;
  private AreaRatioSeverity severityStrategy;

  @BeforeEach
  void setUp() {
    thresholds =
        new Thresholds(
            0.05, // severityAreaRatioLowMax
            0.15, // severityAreaRatioMediumMax
            1, // severityEscalationPothole
            1, // severityEscalationAlligator
            0, // severityEscalationLinearCrack
            15.0, 1.0, 40.0, 30.0, 0.70, 640, 480, 100.0, 40, 220, 10, 15.0, 0.65, 0.25, 3, 20.0,
            0.25);
    severityStrategy = new AreaRatioSeverity(thresholds);
  }

  private LocationFix dummyLocation() {
    return new LocationFix(LocationSource.EXIF, new GeoPoint(13.0, 80.0), 5.0);
  }

  private Observation createObs(double areaRatio, DamageClass damageClass) {
    int frameW = 1000;
    int frameH = 1000; // Total frame area = 1,000,000
    int targetArea = (int) Math.round(areaRatio * 1_000_000);
    // Construct box with approximately targetArea
    int side = (int) Math.round(Math.sqrt(targetArea));
    int x1 = 100;
    int y1 = 100;
    int x2 = x1 + side;
    int y2 = y1 + (targetArea / side);
    return new Observation(
        "hash", damageClass, 0.9, new BoundingBox(x1, y1, x2, y2), frameW, frameH);
  }

  @Test
  @DisplayName("Linear crack with area ratio below low threshold stays LOW (no escalation)")
  void linearCrackBelowLowThreshold() {
    Defect crack = new LongitudinalCrack("c1", dummyLocation());
    Observation obs = createObs(0.04, DamageClass.CRACK_LINEAR); // 4% <= 5%
    assertThat(severityStrategy.calculateSeverity(crack, obs)).isEqualTo(SeverityLevel.LOW);
  }

  @Test
  @DisplayName("Linear crack exactly at low threshold stays LOW")
  void linearCrackAtLowThreshold() {
    Defect crack = new LongitudinalCrack("c2", dummyLocation());
    Observation obs = createObs(0.05, DamageClass.CRACK_LINEAR); // Exactly 5%
    assertThat(severityStrategy.calculateSeverity(crack, obs)).isEqualTo(SeverityLevel.LOW);
  }

  @Test
  @DisplayName("Linear crack above low threshold and below medium stays MEDIUM")
  void linearCrackBetweenLowAndMedium() {
    Defect crack = new LongitudinalCrack("c3", dummyLocation());
    Observation obs = createObs(0.10, DamageClass.CRACK_LINEAR); // 10%
    assertThat(severityStrategy.calculateSeverity(crack, obs)).isEqualTo(SeverityLevel.MEDIUM);
  }

  @Test
  @DisplayName("Linear crack exactly at medium threshold stays MEDIUM")
  void linearCrackAtMediumThreshold() {
    Defect crack = new LongitudinalCrack("c4", dummyLocation());
    Observation obs = createObs(0.15, DamageClass.CRACK_LINEAR); // 15%
    assertThat(severityStrategy.calculateSeverity(crack, obs)).isEqualTo(SeverityLevel.MEDIUM);
  }

  @Test
  @DisplayName("Linear crack above medium threshold is HIGH")
  void linearCrackAboveMediumThreshold() {
    Defect crack = new LongitudinalCrack("c5", dummyLocation());
    Observation obs = createObs(0.16, DamageClass.CRACK_LINEAR); // 16%
    assertThat(severityStrategy.calculateSeverity(crack, obs)).isEqualTo(SeverityLevel.HIGH);
  }

  @Test
  @DisplayName("Pothole escalates LOW to MEDIUM (+1)")
  void potholeEscalatesLowToMedium() {
    Defect pothole = new Pothole("p1", dummyLocation());
    Observation obs = createObs(0.04, DamageClass.POTHOLE); // Base LOW -> Escalated MEDIUM
    assertThat(severityStrategy.calculateSeverity(pothole, obs)).isEqualTo(SeverityLevel.MEDIUM);
  }

  @Test
  @DisplayName("Pothole escalates MEDIUM to HIGH (+1)")
  void potholeEscalatesMediumToHigh() {
    Defect pothole = new Pothole("p2", dummyLocation());
    Observation obs = createObs(0.10, DamageClass.POTHOLE); // Base MEDIUM -> Escalated HIGH
    assertThat(severityStrategy.calculateSeverity(pothole, obs)).isEqualTo(SeverityLevel.HIGH);
  }

  @Test
  @DisplayName("Pothole escalates HIGH capped at HIGH")
  void potholeEscalatesHighCappedAtHigh() {
    Defect pothole = new Pothole("p3", dummyLocation());
    Observation obs = createObs(0.20, DamageClass.POTHOLE); // Base HIGH -> Capped HIGH
    assertThat(severityStrategy.calculateSeverity(pothole, obs)).isEqualTo(SeverityLevel.HIGH);
  }

  @Test
  @DisplayName("Alligator crack escalates LOW to MEDIUM (+1)")
  void alligatorEscalatesLowToMedium() {
    Defect alligator = new AlligatorCrack("a1", dummyLocation());
    Observation obs = createObs(0.03, DamageClass.ALLIGATOR);
    assertThat(severityStrategy.calculateSeverity(alligator, obs)).isEqualTo(SeverityLevel.MEDIUM);
  }

  @Test
  @DisplayName(
      "BestViewSeverity automatically selects best observation and sets severity on defect")
  void bestViewSeveritySelectsAndSetsOnDefect() {
    BestViewSeverity bestViewSeverity = new BestViewSeverity(thresholds);
    Defect pothole = new Pothole("p-auto", dummyLocation());

    Observation smallObs =
        new Observation(
            "h1",
            DamageClass.POTHOLE,
            0.8,
            new BoundingBox(100, 100, 200, 200),
            1000,
            1000); // 10,000 area = 1%
    Observation largeObs =
        new Observation(
            "h2",
            DamageClass.POTHOLE,
            0.85,
            new BoundingBox(100, 100, 450, 450),
            1000,
            1000); // 122,500 area = 12.25%

    pothole.addObservation(smallObs);
    pothole.addObservation(largeObs);

    // largeObs is 12.25% (base MEDIUM), pothole escalates +1 -> HIGH
    SeverityLevel level = bestViewSeverity.calculateSeverity(pothole);
    assertThat(level).isEqualTo(SeverityLevel.HIGH);
    assertThat(pothole.getSeverity()).contains(SeverityLevel.HIGH);
  }
}
