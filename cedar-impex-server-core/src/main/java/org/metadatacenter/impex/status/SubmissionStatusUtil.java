package org.metadatacenter.impex.status;

public class SubmissionStatusUtil {

  public static String getShortStatusMessage(String submissionId, SubmissionState state) {
    return "Submission ID: " + submissionId + "\n" + "Status: " + state + "\n";
  }
}
