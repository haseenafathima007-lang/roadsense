package com.roadai.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainRecordTest {

  @Test
  void geoPoint_validatesBounds() {
    assertThat(new GeoPoint(0, 0)).isNotNull();
    assertThatThrownBy(() -> new GeoPoint(91, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new GeoPoint(0, -181)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void boundingBox_validatesCoordinates() {
    BoundingBox box = new BoundingBox(10, 10, 100, 100);
    assertThat(box.width()).isEqualTo(90);
    assertThat(box.height()).isEqualTo(90);
    assertThat(box.area()).isEqualTo(8100);

    assertThatThrownBy(() -> new BoundingBox(-1, 0, 10, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new BoundingBox(10, 10, 5, 20))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void locationFix_validatesArguments() {
    GeoPoint point = new GeoPoint(45.0, 90.0);
    LocationFix fix = new LocationFix(LocationSource.EXIF, point, 15.0);
    assertThat(fix.accuracyM()).isEqualTo(15.0);

    assertThatThrownBy(() -> new LocationFix(null, point, 15.0))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new LocationFix(LocationSource.DEVICE, point, -5.0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void observation_validatesBounds() {
    BoundingBox box = new BoundingBox(0, 0, 100, 100);
    Observation obs = new Observation(DamageClass.POTHOLE, 0.9, box, 640, 480);

    assertThat(obs.damageClass()).isEqualTo(DamageClass.POTHOLE);

    assertThatThrownBy(() -> new Observation(DamageClass.POTHOLE, 1.1, box, 640, 480))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new Observation(DamageClass.POTHOLE, 0.5, box, 50, 50))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("exceeds frame dimensions");
  }

  @Test
  void report_managesObservationsList() {
    LocationFix fix = new LocationFix(LocationSource.EXIF, new GeoPoint(1, 1), 5.0);
    Report report = new Report("hash", Instant.now(), fix, null, List.of());

    assertThat(report.getObservations()).isEmpty();

    Observation obs =
        new Observation(DamageClass.PATCH, 0.5, new BoundingBox(0, 0, 10, 10), 100, 100);
    report.addObservation(obs);

    assertThat(report.getObservations()).hasSize(1);

    assertThatThrownBy(() -> report.getObservations().add(obs))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
