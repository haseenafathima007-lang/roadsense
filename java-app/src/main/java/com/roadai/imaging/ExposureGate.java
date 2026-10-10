package com.roadai.imaging;

import com.roadai.domain.Observation;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.List;
import javax.imageio.ImageIO;

public class ExposureGate implements QualityGate {
  private final int minMeanLuma;
  private final int maxMeanLuma;

  public ExposureGate(int minMeanLuma, int maxMeanLuma) {
    this.minMeanLuma = minMeanLuma;
    this.maxMeanLuma = maxMeanLuma;
  }

  @Override
  public GateResult check(ImageInfo info, List<Observation> observations) {
    try {
      BufferedImage image = ImageIO.read(info.path().toFile());
      if (image == null) {
        return GateResult.failure("Could not read image for exposure check");
      }

      int w = image.getWidth();
      int h = image.getHeight();
      BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
      gray.getGraphics().drawImage(image, 0, 0, null);
      Raster raster = gray.getRaster();

      long sum = 0;
      int count = w * h;
      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          sum += raster.getSample(x, y, 0);
        }
      }

      double meanLuma = (double) sum / count;

      if (meanLuma < minMeanLuma) {
        return GateResult.failure(
            String.format("Image too dark (mean luma %.1f < %d)", meanLuma, minMeanLuma));
      }
      if (meanLuma > maxMeanLuma) {
        return GateResult.failure(
            String.format("Image overexposed (mean luma %.1f > %d)", meanLuma, maxMeanLuma));
      }
      return GateResult.success();
    } catch (Exception e) {
      return GateResult.failure("Failed to process image for exposure check: " + e.getMessage());
    }
  }
}
