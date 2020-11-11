package org.metadatacenter.submission.immport;

import org.metadatacenter.submission.status.SubmissionStatus;
import org.metadatacenter.submission.status.SubmissionStatusTask;
import org.metadatacenter.submission.status.SubmissionType;

public class ImmPortSubmissionStatusTask extends SubmissionStatusTask {

  private ImmPortUtil immPortUtil;

  public ImmPortSubmissionStatusTask(String submissionID, SubmissionType submissionType, String userID, String
      statusURL, ImmPortUtil immPortUtil) {
    super(submissionID, submissionType, userID, statusURL);
    this.immPortUtil = immPortUtil;
  }

  @Override
  protected SubmissionStatus callSubmissionStatusEndpoint() {
    return immPortUtil.getImmPortSubmissionStatus(getSubmissionID());
  }
}
