package com.roadai.imaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExifReaderTest {

  @Test
  void emptyFile_returnsEmptyOptionals(@TempDir Path tempDir) throws IOException {
    Path dummy = tempDir.resolve("empty.jpg");
    Files.write(dummy, new byte[] {0, 0, 0});

    MetadataExtractorReader reader = new MetadataExtractorReader();
    ExifResult result = reader.read(dummy);

    assertThat(result.captureTime()).isEmpty();
    assertThat(result.gps()).isEmpty();
    assertThat(result.cameraMake()).isEmpty();
    assertThat(result.cameraModel()).isEmpty();
  }

  @Test
  void missingExif_returnsEmptyOptionals(@TempDir Path tempDir) throws IOException {
    Path noExif = tempDir.resolve("no-exif.jpg");
    BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    ImageIO.write(img, "jpg", noExif.toFile());

    MetadataExtractorReader reader = new MetadataExtractorReader();
    ExifResult result = reader.read(noExif);

    assertThat(result.captureTime()).isEmpty();
    assertThat(result.gps()).isEmpty();
  }
}
