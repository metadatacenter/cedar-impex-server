package org.metadatacenter.submission.status;

public enum SubmissionState {
  SUBMITTED(0), PROCESSING(1), SUCCEEDED(2), REJECTED(3), ERROR(4);

  private final int value;

  SubmissionState(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }
}