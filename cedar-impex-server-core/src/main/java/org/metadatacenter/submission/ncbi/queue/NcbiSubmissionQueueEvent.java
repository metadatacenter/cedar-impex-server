package org.metadatacenter.submission.ncbi.queue;

import org.metadatacenter.constant.CedarConstants;
import org.metadatacenter.submission.ncbi.NcbiSubmission;

import java.time.Instant;

public class NcbiSubmissionQueueEvent {

  private String createdAt;
  private long createdAtTS;
  private NcbiSubmission submission;

  public NcbiSubmissionQueueEvent() {
  }

  public NcbiSubmissionQueueEvent(NcbiSubmission submission) {
    Instant now = Instant.now();
    this.createdAt = CedarConstants.xsdDateTimeFormatter.format(now);
    this.createdAtTS = now.getEpochSecond();
    this.submission = submission;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public long getCreatedAtTS() {
    return createdAtTS;
  }

  public NcbiSubmission getSubmission() {
    return submission;
  }

}
