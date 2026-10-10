package com.roadai.imaging;

import java.nio.file.Path;

public interface ExifReader {
  ExifResult read(Path image);
}
