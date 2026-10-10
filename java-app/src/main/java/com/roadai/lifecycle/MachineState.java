package com.roadai.lifecycle;

public enum MachineState {
  NEW,
  REPORTED_AGAIN,
  NEEDS_REVIEW,
  AFTER_PHOTO_CLEAR,
  AFTER_PHOTO_STILL_DAMAGED,
  AFTER_PHOTO_REJECTED,
  REAPPEARED
}
