package com.roadai.perception;

import com.roadai.domain.Observation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeDetector implements DamageDetector {
  private final List<Observation> scriptedOutputs;
  private boolean shouldFail;

  public FakeDetector(List<Observation> scriptedOutputs) {
    this.scriptedOutputs = new ArrayList<>(scriptedOutputs);
    this.shouldFail = false;
  }

  public void setShouldFail(boolean shouldFail) {
    this.shouldFail = shouldFail;
  }

  @Override
  public List<Observation> detect(Path image) throws ApiException {
    if (shouldFail) {
      throw new ApiException(500, "INTERNAL_ERROR", "Simulated failure");
    }
    return Collections.unmodifiableList(scriptedOutputs);
  }
}
