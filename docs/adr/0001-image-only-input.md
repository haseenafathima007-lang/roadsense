# ADR 0001: Input is images only
Status: accepted. Context: video needs a tracker, time sync and long jobs; training data (RDD2022, Chennai set) are single images.
Decision: remove video; use multi-photo dedup, location pipeline, best-view selection, proof-of-visit.
Consequences: no continuous coverage (spot mode reports defects only; ratings only in survey mode); EXIF GPS can be missing or wrong; one-off false positives matter more.
