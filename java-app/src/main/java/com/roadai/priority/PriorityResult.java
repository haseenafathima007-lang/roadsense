package com.roadai.priority;

import java.util.List;

/**
 * The result of a priority calculation for one defect.
 *
 * @param score the final product of all factor values (≥ 0)
 * @param contributions each factor's name and value, for explainability
 */
public record PriorityResult(double score, List<FactorContribution> contributions) {}
