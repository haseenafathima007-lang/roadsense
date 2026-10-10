package com.roadai.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Report {
  private final String fileHash;
  private final Instant captureTime;
  private final LocationFix location;
  private final CaptureMeta meta;
  private final List<Observation> observations;

  public Report(
      String fileHash,
      Instant captureTime,
      LocationFix location,
      CaptureMeta meta,
      List<Observation> observations) {
    this.fileHash = Objects.requireNonNull(fileHash, "File hash cannot be null");
    this.captureTime = Objects.requireNonNull(captureTime, "Capture time cannot be null");
    this.location = location; // Can be null if missing
    this.meta = meta; // Can be null if missing
    this.observations = new ArrayList<>(Objects.requireNonNull(observations));
  }

  public String getFileHash() {
    return fileHash;
  }

  public Instant getCaptureTime() {
    return captureTime;
  }

  public Optional<LocationFix> getLocation() {
    return Optional.ofNullable(location);
  }

  public Optional<CaptureMeta> getMeta() {
    return Optional.ofNullable(meta);
  }

  public List<Observation> getObservations() {
    return Collections.unmodifiableList(observations);
  }

  public void addObservation(Observation observation) {
    observations.add(Objects.requireNonNull(observation));
  }
}
