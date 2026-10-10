package com.roadai.domain;

import com.roadai.config.Thresholds;
import com.roadai.lifecycle.MachineState;
import com.roadai.lifecycle.WorkflowState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class Defect {
  private final String id;
  private final LocationFix location;
  private final List<Observation> observations;

  private MachineState machineState;
  private WorkflowState workflowState;
  private SeverityLevel severity;
  private Double priority;
  private String roadLinkId;
  private boolean poorViewOnly;

  protected Defect(String id, LocationFix location) {
    this.id = Objects.requireNonNull(id, "ID cannot be null");
    this.location = Objects.requireNonNull(location, "Location cannot be null");
    this.observations = new ArrayList<>();
    this.machineState = MachineState.NEW;
    this.workflowState = WorkflowState.UNREVIEWED;
  }

  public String getId() {
    return id;
  }

  public LocationFix getLocation() {
    return location;
  }

  public List<Observation> getObservations() {
    return Collections.unmodifiableList(observations);
  }

  public void addObservation(Observation observation) {
    Objects.requireNonNull(observation, "Observation cannot be null");
    this.observations.add(observation);
  }

  public MachineState getMachineState() {
    return machineState;
  }

  public void setMachineState(MachineState machineState) {
    this.machineState = Objects.requireNonNull(machineState);
  }

  public WorkflowState getWorkflowState() {
    return workflowState;
  }

  public void setWorkflowState(WorkflowState workflowState) {
    this.workflowState = Objects.requireNonNull(workflowState);
  }

  public Optional<SeverityLevel> getSeverity() {
    return Optional.ofNullable(severity);
  }

  public void setSeverity(SeverityLevel severity) {
    this.severity = severity;
  }

  public Optional<Double> getPriority() {
    return Optional.ofNullable(priority);
  }

  public void setPriority(Double priority) {
    this.priority = priority;
  }

  public String getRoadLinkId() {
    return roadLinkId;
  }

  public void setRoadLinkId(String roadLinkId) {
    this.roadLinkId = roadLinkId;
  }

  public boolean isPoorViewOnly() {
    return poorViewOnly;
  }

  public void setPoorViewOnly(boolean poorViewOnly) {
    this.poorViewOnly = poorViewOnly;
  }

  public abstract int escalation(Thresholds thresholds);

  public abstract DamageClass getDamageClass();
}
