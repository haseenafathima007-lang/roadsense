package com.roadai.perception;

import com.roadai.domain.Observation;
import java.nio.file.Path;
import java.util.List;

public interface DamageDetector {
  List<Observation> detect(Path image) throws ApiException;
}
