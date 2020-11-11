package org.metadatacenter.submission.status;

public enum SubmissionType {
  IMMPORT("ImmPort"), NCBI("NCBI");

  private final String value;

  SubmissionType(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}