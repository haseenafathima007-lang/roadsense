package com.roadai.imaging;

import com.roadai.domain.Observation;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.List;
import javax.imageio.ImageIO;

public class BlurGate implements QualityGate {
  private final double minLaplacianVar;

  public BlurGate(double minLaplacianVar) {
    this.minLaplacianVar = minLaplacianVar;
  }

  @Override
  public GateResult check(ImageInfo info, List<Observation> observations) {
    try {
      BufferedImage image = ImageIO.read(info.path().toFile());
      if (image == null) {
        return GateResult.failure("Could not read image for blur check");
      }
      double var = calculateLaplacianVariance(image);
      if (var < minLaplacianVar) {
        return GateResult.failure(
            String.format("Image too blurry (variance %.1f < %.1f)", var, minLaplacianVar));
      }
      return GateResult.success();
    } catch (Exception e) {
      return GateResult.failure("Failed to process image for blur check: " + e.getMessage());
    }
  }

  private double calculateLaplacianVariance(BufferedImage image) {
    int w = image.getWidth();
    int h = image.getHeight();

    // Convert to grayscale if necessary or just extract luminance
    BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
    gray.getGraphics().drawImage(image, 0, 0, null);
    Raster raster = gray.getRaster();

    // 3x3 Laplacian kernel:
    //  0  1  0
    //  1 -4  1
    //  0  1  0

    long sum = 0;
    long sumSq = 0;
    int count = 0;

    for (int y = 1; y < h - 1; y++) {
      for (int x = 1; x < w - 1; x++) {
        int p01 = raster.getSample(x, y - 1, 0);
        int p10 = raster.getSample(x - 1, y, 0);
        int p11 = raster.getSample(x, y, 0);
        int p12 = raster.getSample(x + 1, y, 0);
        int p21 = raster.getSample(x, y + 1, 0);

        int laplacian = p01 + p10 + p12 + p21 - 4 * p11;
        sum += laplacian;
        sumSq += (long) laplacian * laplacian;
        count++;
      }
    }

    if (count == 0) return 0;

    double mean = (double) sum / count;
    return ((double) sumSq / count) - (mean * mean);
  }
}
