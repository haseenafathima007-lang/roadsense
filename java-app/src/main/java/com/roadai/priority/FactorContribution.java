package com.roadai.priority;

/**
 * One factor's contribution to a defect's priority score.
 *
 * @param name the factor's human-readable name
 * @param value the factor's numeric multiplier value
 */
public record FactorContribution(String name, double value) {}
