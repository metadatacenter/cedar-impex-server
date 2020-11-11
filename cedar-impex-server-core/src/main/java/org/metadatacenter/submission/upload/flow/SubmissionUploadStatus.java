package org.metadatacenter.submission.upload.flow;

import java.util.Map;

public class SubmissionUploadStatus {

  private long totalFilesCount;
  private long uploadedFilesCount;
  private Map<String, FileUploadStatus> filesUploadStatus;
  private String submissionLocalPath;

  public SubmissionUploadStatus(long totalFilesCount, long uploadedFilesCount, Map<String, FileUploadStatus>
      filesUploadStatus, String submissionLocalPath) {
    this.totalFilesCount = totalFilesCount;
    this.uploadedFilesCount = uploadedFilesCount;
    this.filesUploadStatus = filesUploadStatus;
    this.submissionLocalPath = submissionLocalPath;
  }

  public long getTotalFilesCount() {
    return totalFilesCount;
  }

  public void setTotalFilesCount(long totalFilesCount) {
    this.totalFilesCount = totalFilesCount;
  }

  public long getUploadedFilesCount() {
    return uploadedFilesCount;
  }

  public void setUploadedFilesCount(long uploadedFilesCount) {
    this.uploadedFilesCount = uploadedFilesCount;
  }

  public Map<String, FileUploadStatus> getFilesUploadStatus() {
    return filesUploadStatus;
  }

  public void setFilesUploadStatus(Map<String, FileUploadStatus> filesUploadStatus) {
    this.filesUploadStatus = filesUploadStatus;
  }

  public String getSubmissionLocalPath() {
    return submissionLocalPath;
  }

  public void setSubmissionLocalPath(String submissionLocalPath) {
    this.submissionLocalPath = submissionLocalPath;
  }
}
