package org.metadatacenter.submission.ncbi.status.report;

public enum NcbiSubmissionState {
  SUBMITTED("submitted"), PROCESSING("processing"), PROCESSED_ERROR("processed-error"), FAILED("failed");

  private final String value;

  NcbiSubmissionState(String value) {
    this.value = value;
  }

  public static NcbiSubmissionState fromString(String state) {
    for (NcbiSubmissionState s : NcbiSubmissionState.values()) {
      if (s.getValue().equals(state)) {
        return s;
      }
    }
    return null;
  }

  public String getValue() {
    return value;
  }

}