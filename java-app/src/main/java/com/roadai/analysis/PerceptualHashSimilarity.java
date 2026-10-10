package com.roadai.analysis;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

public class PerceptualHashSimilarity implements ImageSimilarity {

  @Override
  public double score(Path imageA, Path imageB) {
    try {
      BufferedImage imgA = ImageIO.read(imageA.toFile());
      BufferedImage imgB = ImageIO.read(imageB.toFile());

      if (imgA == null || imgB == null) {
        return 0.0;
      }

      long hashA = calcDHash(imgA);
      long hashB = calcDHash(imgB);

      int distance = Long.bitCount(hashA ^ hashB);

      // 64 bits total. 0 distance = 1.0 score, 64 distance = 0.0 score
      return 1.0 - ((double) distance / 64.0);
    } catch (IOException e) {
      return 0.0;
    }
  }

  private long calcDHash(BufferedImage image) {
    // Resize to 9x8
    Image scaled = image.getScaledInstance(9, 8, Image.SCALE_SMOOTH);
    BufferedImage resized = new BufferedImage(9, 8, BufferedImage.TYPE_BYTE_GRAY);
    Graphics2D g2d = resized.createGraphics();
    g2d.drawImage(scaled, 0, 0, null);
    g2d.dispose();

    long hash = 0;
    int bitIndex = 0;

    for (int y = 0; y < 8; y++) {
      for (int x = 0; x < 8; x++) {
        int leftPixel = resized.getRGB(x, y) & 0xFF;
        int rightPixel = resized.getRGB(x + 1, y) & 0xFF;

        if (leftPixel < rightPixel) {
          hash |= (1L << bitIndex);
        }
        bitIndex++;
      }
    }

    return hash;
  }
}
