package com.roadai.priority;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.config.Thresholds;
import com.roadai.domain.BoundingBox;
import com.roadai.domain.DamageClass;
import com.roadai.domain.Defect;
import com.roadai.domain.GeoPoint;
import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import com.roadai.domain.Observation;
import com.roadai.domain.Pothole;
import com.roadai.domain.SeverityLevel;
import com.roadai.geo.OsmContext;
import com.roadai.geo.RoadLink;
import com.roadai.geo.RoadLinkSnapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PriorityCalculatorTest {

  private static final Map<String, Double> ROAD_WEIGHTS =
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

  private Thresholds thresholds;
  private Context context;

  @BeforeEach
  void setUp() {
    thresholds =
        new Thresholds(
            0.05,
            0.15,
            1,
            1,
            0,
            15.0,
            1.0,
            40.0,
            30.0,
            0.70,
            640,
            480,
            100.0,
            40,
            220,
            10,
            15.0,
            0.65,
            0.25,
            3,
            20.0,
            0.25,
            ROAD_WEIGHTS,
            0.2,
            0.2,
            1.0,
            50.0,
            10);
    // Empty OSM — no road links present
    OsmContext emptyOsm = (point, radiusM) -> List.of();
    context = new Context(thresholds, emptyOsm);
  }

  private static LocationFix exifFix(double lat, double lon) {
    return new LocationFix(LocationSource.EXIF, new GeoPoint(lat, lon), 5.0);
  }

  private static Observation obs(String hash) {
    return new Observation(
        hash, DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 640, 640);
  }

  // ── SeverityFactor ───────────────────────────────────────────────────────

  @Test
  @DisplayName("SeverityFactor: LOW → 1.0, MEDIUM → 2.0, HIGH → 3.0")
  void severityFactorValues() {
    SeverityFactor sf = new SeverityFactor();

    Pothole low = new Pothole("p1", exifFix(13, 80));
    low.setSeverity(SeverityLevel.LOW);
    assertThat(sf.value(low, context)).isEqualTo(1.0);

    Pothole med = new Pothole("p2", exifFix(13, 80));
    med.setSeverity(SeverityLevel.MEDIUM);
    assertThat(sf.value(med, context)).isEqualTo(2.0);

    Pothole high = new Pothole("p3", exifFix(13, 80));
    high.setSeverity(SeverityLevel.HIGH);
    assertThat(sf.value(high, context)).isEqualTo(3.0);
  }

  @Test
  @DisplayName("SeverityFactor: absent severity → 0.0")
  void severityFactorAbsentYieldsZero() {
    SeverityFactor sf = new SeverityFactor();
    Pothole noSev = new Pothole("p", exifFix(13, 80));
    assertThat(sf.value(noSev, context)).isEqualTo(0.0);
  }

  // ── RoadClassFactor ──────────────────────────────────────────────────────

  @Test
  @DisplayName("RoadClassFactor: motorway link snapped → weight 3.0")
  void roadClassFactorMotorway() {
    RoadLink motorway =
        new RoadLink(
            "w1", "motorway", List.of(new GeoPoint(13.0, 80.0), new GeoPoint(13.001, 80.0)));
    OsmContext osmWithMotorway = (point, r) -> List.of(motorway);
    Context ctx = new Context(thresholds, osmWithMotorway);

    RoadLinkSnapper snapper = new RoadLinkSnapper();
    RoadClassFactor rcf = new RoadClassFactor(snapper);
    Pothole p =
        new Pothole("p", new LocationFix(LocationSource.EXIF, new GeoPoint(13.0005, 80.0), 5.0));
    p.setSeverity(SeverityLevel.HIGH);

    assertThat(rcf.value(p, ctx)).isEqualTo(3.0);
  }

  @Test
  @DisplayName("RoadClassFactor: unrecognised highway class → neutral 1.0")
  void roadClassFactorUnknown() {
    RoadLink unknown =
        new RoadLink(
            "w1", "service", List.of(new GeoPoint(13.0, 80.0), new GeoPoint(13.001, 80.0)));
    OsmContext osmWithUnknown = (point, r) -> List.of(unknown);
    Context ctx = new Context(thresholds, osmWithUnknown);

    RoadClassFactor rcf = new RoadClassFactor(new RoadLinkSnapper());
    Pothole p =
        new Pothole("p", new LocationFix(LocationSource.EXIF, new GeoPoint(13.0005, 80.0), 5.0));

    assertThat(rcf.value(p, ctx)).isEqualTo(1.0);
  }

  @Test
  @DisplayName("RoadClassFactor: no nearby links → neutral 1.0")
  void roadClassFactorNoNearbyLinks() {
    RoadClassFactor rcf = new RoadClassFactor(new RoadLinkSnapper());
    Pothole p = new Pothole("p", exifFix(13, 80));
    // context has emptyOsm → no snap → 1.0
    assertThat(rcf.value(p, context)).isEqualTo(1.0);
  }

  // ── RecurrenceFactor ─────────────────────────────────────────────────────

  @Test
  @DisplayName("RecurrenceFactor: 1 observation → 1.0 (neutral)")
  void recurrenceFactorOneObservation() {
    RecurrenceFactor rf = new RecurrenceFactor();
    Pothole p = new Pothole("p", exifFix(13, 80));
    p.addObservation(obs("h1"));
    assertThat(rf.value(p, context)).isEqualTo(1.0);
  }

  @Test
  @DisplayName("RecurrenceFactor: 3 observations → 1.0 + 2×0.2 = 1.4")
  void recurrenceFactorThreeObservations() {
    RecurrenceFactor rf = new RecurrenceFactor();
    Pothole p = new Pothole("p", exifFix(13, 80));
    p.addObservation(obs("h1"));
    p.addObservation(obs("h2"));
    p.addObservation(obs("h3"));
    assertThat(rf.value(p, context)).isEqualTo(1.4, org.assertj.core.data.Offset.offset(1e-9));
  }

  // ── CorroborationFactor ──────────────────────────────────────────────────

  @Test
  @DisplayName("CorroborationFactor: 2 distinct hashes → 1.0 + 1×0.2 = 1.2")
  void corroborationFactorTwoReporters() {
    CorroborationFactor cf = new CorroborationFactor();
    Pothole p = new Pothole("p", exifFix(13, 80));
    p.addObservation(obs("hash-A"));
    p.addObservation(obs("hash-B"));
    assertThat(cf.value(p, context)).isEqualTo(1.2, org.assertj.core.data.Offset.offset(1e-9));
  }

  // ── PriorityCalculator ───────────────────────────────────────────────────

  @Test
  @DisplayName("Full score: HIGH severity, 2 obs, no road snap → 3×1×1×1.2 = 3.6")
  void fullScoreHandComputed() {
    PriorityCalculator calc =
        new PriorityCalculator(
            List.of(
                new SeverityFactor(),
                new RoadClassFactor(new RoadLinkSnapper()),
                new ExposureFactor(),
                new RecurrenceFactor()));

    Pothole p = new Pothole("p", exifFix(13, 80));
    p.setSeverity(SeverityLevel.HIGH);
    p.addObservation(obs("h1"));
    p.addObservation(obs("h2"));

    // 3.0 (HIGH) × 1.0 (no road link) × 1.0 (exposure default) × 1.2 (2 obs) = 3.6
    PriorityResult result = calc.calculate(p, context);
    assertThat(result.score()).isEqualTo(3.6, org.assertj.core.data.Offset.offset(1e-9));
    assertThat(result.contributions()).hasSize(4);
    assertThat(result.contributions().get(0)).isEqualTo(new FactorContribution("severity", 3.0));
  }

  @Test
  @DisplayName("Zero severity → score = 0 (ranked last)")
  void zeroSeverityYieldsZeroScore() {
    PriorityCalculator calc =
        new PriorityCalculator(List.of(new SeverityFactor(), new RecurrenceFactor()));
    Pothole p = new Pothole("p", exifFix(13, 80));
    // No severity set
    assertThat(calc.calculate(p, context).score()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("Negative factor clamped to 0 — product becomes 0")
  void negativeFactorClamped() {
    PriorityFactor badFactor =
        new PriorityFactor() {
          @Override
          public String name() {
            return "bad";
          }

          @Override
          public double value(Defect d, Context ctx) {
            return -5.0;
          }
        };
    PriorityCalculator calc = new PriorityCalculator(List.of(badFactor));
    Pothole p = new Pothole("p", exifFix(13, 80));
    PriorityResult result = calc.calculate(p, context);
    assertThat(result.score()).isEqualTo(0.0);
    assertThat(result.contributions().get(0).value()).isEqualTo(0.0);
  }

  @Test
  @DisplayName("New factor registered without editing any existing class (open/closed proof)")
  void newFactorDoesNotEditExistingClasses() {
    // A brand-new factor type — does not change PriorityFactor, SeverityFactor, or
    // PriorityCalculator
    PriorityFactor tagFactor =
        new PriorityFactor() {
          @Override
          public String name() {
            return "tag_boost";
          }

          @Override
          public double value(Defect d, Context ctx) {
            return 1.5;
          }
        };

    PriorityCalculator calc = new PriorityCalculator(List.of(new SeverityFactor(), tagFactor));
    Pothole p = new Pothole("p", exifFix(13, 80));
    p.setSeverity(SeverityLevel.MEDIUM);

    // 2.0 × 1.5 = 3.0
    PriorityResult result = calc.calculate(p, context);
    assertThat(result.score()).isEqualTo(3.0, org.assertj.core.data.Offset.offset(1e-9));
    assertThat(result.contributions()).extracting(FactorContribution::name).contains("tag_boost");
  }

  @Test
  @DisplayName("rankForMap excludes defects with no location (via null-override)")
  void noLocationExcludedFromRankedMap() {
    // Simulate a null-location defect via anonymous subclass override
    Defect noLocDefect =
        new Pothole("no-loc", exifFix(13, 80)) {
          @Override
          public LocationFix getLocation() {
            return null;
          }
        };

    Pothole withLoc = new Pothole("with-loc", exifFix(13, 80));
    withLoc.setSeverity(SeverityLevel.HIGH);

    PriorityCalculator calc = new PriorityCalculator(List.of(new SeverityFactor()));
    List<Defect> ranked = calc.rankForMap(List.of(noLocDefect, withLoc), context);
    assertThat(ranked).containsExactly(withLoc);
  }

  @Test
  @DisplayName("locationNeeded returns only defects with null location")
  void locationNeededListContainsNullLocation() {
    Defect noLocDefect =
        new Pothole("no-loc", exifFix(13, 80)) {
          @Override
          public LocationFix getLocation() {
            return null;
          }
        };
    Pothole withLoc = new Pothole("with-loc", exifFix(13, 80));

    PriorityCalculator calc = new PriorityCalculator(List.of(new SeverityFactor()));
    assertThat(calc.locationNeeded(List.of(noLocDefect, withLoc))).containsExactly(noLocDefect);
  }

  @Test
  @DisplayName("rankForMap ordered descending by score")
  void rankForMapOrderedDescendingScore() {
    PriorityCalculator calc = new PriorityCalculator(List.of(new SeverityFactor()));

    Pothole low = new Pothole("low", exifFix(13, 80));
    low.setSeverity(SeverityLevel.LOW);

    Pothole high = new Pothole("high", exifFix(13, 80));
    high.setSeverity(SeverityLevel.HIGH);

    Pothole med = new Pothole("med", exifFix(13, 80));
    med.setSeverity(SeverityLevel.MEDIUM);

    List<Defect> ranked = calc.rankForMap(List.of(low, high, med), context);
    assertThat(ranked).extracting(Defect::getId).containsExactly("high", "med", "low");
  }
}
