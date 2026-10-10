package com.roadai.priority;

import com.roadai.config.Thresholds;
import com.roadai.geo.OsmContext;

/**
 * Runtime context passed to every {@link PriorityFactor}.
 *
 * @param thresholds loaded config thresholds
 * @param osmContext cached OSM road link data
 */
public record Context(Thresholds thresholds, OsmContext osmContext) {}
