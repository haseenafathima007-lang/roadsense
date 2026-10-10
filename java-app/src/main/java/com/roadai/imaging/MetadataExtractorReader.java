package com.roadai.imaging;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.roadai.domain.GeoPoint;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

public class MetadataExtractorReader implements ExifReader {

  @Override
  public ExifResult read(Path image) {
    try {
      Metadata metadata = ImageMetadataReader.readMetadata(image.toFile());

      Optional<Instant> captureTime = getCaptureTime(metadata);
      Optional<GeoPoint> gps = getGps(metadata);
      Optional<String> make = getMake(metadata);
      Optional<String> model = getModel(metadata);

      return new ExifResult(captureTime, gps, make, model);
    } catch (Exception e) {
      // Tolerate errors (e.g. missing metadata, unsupported file)
      return new ExifResult(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
  }

  private Optional<Instant> getCaptureTime(Metadata metadata) {
    ExifSubIFDDirectory dir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
    if (dir != null) {
      Date date = dir.getDateOriginal();
      if (date != null) {
        return Optional.of(date.toInstant());
      }
    }
    return Optional.empty();
  }

  private Optional<GeoPoint> getGps(Metadata metadata) {
    GpsDirectory dir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
    if (dir != null && dir.getGeoLocation() != null) {
      double lat = dir.getGeoLocation().getLatitude();
      double lon = dir.getGeoLocation().getLongitude();
      if (!dir.getGeoLocation().isZero()) {
        try {
          return Optional.of(new GeoPoint(lat, lon));
        } catch (IllegalArgumentException e) {
          // Invalid bounds
        }
      }
    }
    return Optional.empty();
  }

  private Optional<String> getMake(Metadata metadata) {
    ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
    if (dir != null) {
      return Optional.ofNullable(dir.getString(ExifIFD0Directory.TAG_MAKE));
    }
    return Optional.empty();
  }

  private Optional<String> getModel(Metadata metadata) {
    ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
    if (dir != null) {
      return Optional.ofNullable(dir.getString(ExifIFD0Directory.TAG_MODEL));
    }
    return Optional.empty();
  }
}
