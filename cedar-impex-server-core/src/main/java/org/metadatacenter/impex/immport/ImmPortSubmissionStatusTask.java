package org.metadatacenter.impex.immport;

import org.metadatacenter.impex.status.SubmissionStatus;
import org.metadatacenter.impex.status.SubmissionStatusTask;
import org.metadatacenter.impex.status.SubmissionType;

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
