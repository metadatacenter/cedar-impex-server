package org.metadatacenter.submission.status;

public class SubmissionStatus {
  private final String submissionID;
  private final SubmissionState submissionState;
  private final String statusMessage;

  public SubmissionStatus(String submissionID, SubmissionState submissionState, String statusMessage) {
    this.submissionID = submissionID;
    this.submissionState = submissionState;
    this.statusMessage = statusMessage;
  }

  public String getSubmissionID() {
    return submissionID;
  }

  public SubmissionState getSubmissionState() {
    return submissionState;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  @Override
  public String toString() {
    return "SubmissionStatus{" +
        "submissionID='" + submissionID + '\'' +
        ", submissionState=" + submissionState +
        ", statusMessage='" + statusMessage + '\'' +
        '}';
  }

  public String getSummary() {
    return "SubmissionStatus{submissionID='" + submissionID + " , submissionState=" + submissionState + '}';
  }
}
