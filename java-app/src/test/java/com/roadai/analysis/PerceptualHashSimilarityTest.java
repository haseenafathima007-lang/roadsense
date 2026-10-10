package com.roadai.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PerceptualHashSimilarityTest {

  @TempDir Path tempDir;

  private final PerceptualHashSimilarity similarity = new PerceptualHashSimilarity();

  private Path createStripedImage(String name, boolean invert) throws IOException {
    BufferedImage img = new BufferedImage(90, 80, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    for (int x = 0; x < 90; x += 10) {
      boolean white = ((x / 10) % 2 == 0) ^ invert;
      g.setColor(white ? Color.WHITE : Color.BLACK);
      g.fillRect(x, 0, 10, 80);
    }
    g.dispose();

    Path path = tempDir.resolve(name + ".png");
    ImageIO.write(img, "png", path.toFile());
    return path;
  }

  @Test
  @DisplayName("Identical images produce a similarity score of 1.0")
  void identicalImagesScoreOne() throws IOException {
    Path p1 = createStripedImage("img1", false);
    Path p2 = createStripedImage("img2", false);

    double score = similarity.score(p1, p2);
    assertThat(score).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Opposite striped images produce low similarity score")
  void oppositeImagesScoreLow() throws IOException {
    Path p1 = createStripedImage("normal", false);
    Path p2 = createStripedImage("inverted", true);

    double score = similarity.score(p1, p2);
    assertThat(score).isLessThan(0.2);
  }

  @Test
  @DisplayName("Missing or invalid image path safely returns 0.0 without exception")
  void missingImageReturnsZero() {
    Path missing = tempDir.resolve("missing.png");
    Path exists = tempDir.resolve("exists.png");

    double score = similarity.score(missing, exists);
    assertThat(score).isEqualTo(0.0);
  }
}
