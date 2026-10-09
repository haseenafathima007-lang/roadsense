# Patch Class Plan

This is a placeholder describing how a "patch" hard-negative class will be added to the model training in Phase 2/3.

## Concept
In the future phases, we will introduce a `patch` class that acts as a hard negative to distinguish between unrepaired damage (like potholes and cracks) and repaired sections (patches) which should not trigger an active defect state.

## Implementation (Phase 2/3)
1. **Data Source**: We will use our own photos (e.g. from the Chennai set). We will not invent or hallucinate data.
2. **Labelling**: The class will be explicitly labelled in YOLO format.
3. **Java Integration**: The Java layer will consume `patch` observations to possibly mute or close defects if a patch overlaps with historical damage at the same location.
