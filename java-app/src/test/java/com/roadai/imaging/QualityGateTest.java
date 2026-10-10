package com.roadai.imaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.roadai.domain.BoundingBox;
import com.roadai.domain.DamageClass;
import com.roadai.domain.Observation;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QualityGateTest {

  @Test
  void sizeGate_checksDimensions() {
    SizeGate gate = new SizeGate(640, 480);

    GateResult pass = gate.check(new ImageInfo(800, 600, null), List.of());
    assertThat(pass.pass()).isTrue();

    GateResult fail = gate.check(new ImageInfo(600, 500, null), List.of());
    assertThat(fail.pass()).isFalse();
    assertThat(fail.reasons()).containsExactly("Image resolution too low (600x500 < 640x480)");
  }

  @Test
  void framingGate_checksEdgeMargin() {
    FramingGate gate = new FramingGate(4);
    ImageInfo info = new ImageInfo(100, 100, null);

    Observation ok =
        new Observation(DamageClass.POTHOLE, 0.9, new BoundingBox(10, 10, 50, 50), 100, 100);
    GateResult pass = gate.check(info, List.of(ok));
    assertThat(pass.pass()).isTrue();

    Observation edge =
        new Observation(DamageClass.POTHOLE, 0.9, new BoundingBox(2, 10, 50, 50), 100, 100);
    GateResult fail = gate.check(info, List.of(edge));
    assertThat(fail.pass()).isFalse();
    assertThat(fail.reasons().getFirst()).contains("touches image edge");
  }

  @Test
  void blurGate_checksLaplacianVariance(@TempDir Path tempDir) throws IOException {
    BlurGate gate = new BlurGate(50.0);

    Path sharpPath = tempDir.resolve("sharp.jpg");
    BufferedImage sharp = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = sharp.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, 100, 100);
    g.setColor(Color.BLACK);
    for (int i = 0; i < 100; i += 10) g.drawLine(i, 0, i, 100);
    g.dispose();
    ImageIO.write(sharp, "jpg", sharpPath.toFile());

    GateResult pass = gate.check(new ImageInfo(100, 100, sharpPath), List.of());
    assertThat(pass.pass()).isTrue();

    Path blurPath = tempDir.resolve("blur.jpg");
    BufferedImage blur = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = blur.createGraphics();
    g2.setColor(Color.GRAY);
    g2.fillRect(0, 0, 100, 100);
    g2.dispose();
    ImageIO.write(blur, "jpg", blurPath.toFile());

    GateResult fail = gate.check(new ImageInfo(100, 100, blurPath), List.of());
    assertThat(fail.pass()).isFalse();
    assertThat(fail.reasons().getFirst()).contains("too blurry");
  }

  @Test
  void exposureGate_checksMeanLuma(@TempDir Path tempDir) throws IOException {
    ExposureGate gate = new ExposureGate(40, 225);

    Path darkPath = tempDir.resolve("dark.jpg");
    BufferedImage dark = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = dark.createGraphics();
    g.setColor(new Color(10, 10, 10)); // < 40
    g.fillRect(0, 0, 10, 10);
    g.dispose();
    ImageIO.write(dark, "jpg", darkPath.toFile());

    GateResult failDark = gate.check(new ImageInfo(10, 10, darkPath), List.of());
    assertThat(failDark.pass()).isFalse();
    assertThat(failDark.reasons().getFirst()).contains("too dark");

    Path brightPath = tempDir.resolve("bright.jpg");
    BufferedImage bright = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = bright.createGraphics();
    g2.setColor(new Color(250, 250, 250)); // > 225
    g2.fillRect(0, 0, 10, 10);
    g2.dispose();
    ImageIO.write(bright, "jpg", brightPath.toFile());

    GateResult failBright = gate.check(new ImageInfo(10, 10, brightPath), List.of());
    assertThat(failBright.pass()).isFalse();
    assertThat(failBright.reasons().getFirst()).contains("overexposed");
  }
}
