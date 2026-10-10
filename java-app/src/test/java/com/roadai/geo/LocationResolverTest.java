package com.roadai.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.roadai.domain.GeoPoint;
import com.roadai.domain.LocationFix;
import com.roadai.domain.LocationSource;
import com.roadai.imaging.ExifResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LocationResolverTest {

  @Test
  void resolvesChainInOrder() {
    ExifLocationHandler exif = new ExifLocationHandler();
    DeviceLocationHandler device = new DeviceLocationHandler();
    ManualPinHandler manual = new ManualPinHandler();

    exif.setNext(device);
    device.setNext(manual);

    LocationResolver resolver = exif;

    GeoPoint pExif = new GeoPoint(1, 1);
    GeoPoint pDevice = new GeoPoint(2, 2);
    GeoPoint pManual = new GeoPoint(3, 3);

    // 1. All available -> EXIF
    ExifResult eRes =
        new ExifResult(Optional.empty(), Optional.of(pExif), Optional.empty(), Optional.empty());
    LocationContext ctx1 = new LocationContext(eRes, pDevice, pManual);
    LocationFix fix1 = resolver.resolve(ctx1).orElseThrow();
    assertThat(fix1.source()).isEqualTo(LocationSource.EXIF);
    assertThat(fix1.point()).isEqualTo(pExif);

    // 2. EXIF missing -> DEVICE
    ExifResult eResEmpty =
        new ExifResult(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    LocationContext ctx2 = new LocationContext(eResEmpty, pDevice, pManual);
    LocationFix fix2 = resolver.resolve(ctx2).orElseThrow();
    assertThat(fix2.source()).isEqualTo(LocationSource.DEVICE);
    assertThat(fix2.point()).isEqualTo(pDevice);

    // 3. EXIF, DEVICE missing -> MANUAL_PIN
    LocationContext ctx3 = new LocationContext(eResEmpty, null, pManual);
    LocationFix fix3 = resolver.resolve(ctx3).orElseThrow();
    assertThat(fix3.source()).isEqualTo(LocationSource.MANUAL_PIN);
    assertThat(fix3.point()).isEqualTo(pManual);

    // 4. All missing -> empty
    LocationContext ctx4 = new LocationContext(eResEmpty, null, null);
    assertThat(resolver.resolve(ctx4)).isEmpty();
  }

  @Test
  void haversine_calculatesDistance() {
    GeoPoint p1 = new GeoPoint(51.5007, 0.1246);
    GeoPoint p2 = new GeoPoint(40.6892, 74.0445);
    double d = Haversine.distanceM(p1, p2);
    // Distance is ~ 5570 km
    assertThat(d).isCloseTo(5570000.0, within(20000.0));
  }
}
