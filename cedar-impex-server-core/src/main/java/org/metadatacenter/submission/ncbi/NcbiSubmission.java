package org.metadatacenter.submission.ncbi;

import java.util.Collection;

public class NcbiSubmission {

  private String id;
  private String cedarUserId;
  private Collection<String> localFilePaths;
  private String submissionFolder;
  private boolean uploadSubmitReadyFile;

  public NcbiSubmission() {
  }

  public NcbiSubmission(String id, String cedarUserId, Collection<String> localFilePaths, String
      submissionFolder, boolean uploadSubmitReadyFile) {
    this.id = id;
    this.cedarUserId = cedarUserId;
    this.localFilePaths = localFilePaths;
    this.submissionFolder = submissionFolder;
    this.uploadSubmitReadyFile = uploadSubmitReadyFile;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getCedarUserId() {
    return cedarUserId;
  }

  public void setCedarUserId(String cedarUserId) {
    this.cedarUserId = cedarUserId;
  }

  public Collection<String> getLocalFilePaths() {
    return localFilePaths;
  }

  public void setLocalFilePaths(Collection<String> localFilePaths) {
    this.localFilePaths = localFilePaths;
  }

  public String getSubmissionFolder() {
    return submissionFolder;
  }

  public void setSubmissionFolder(String submissionFolder) {
    this.submissionFolder = submissionFolder;
  }

  public boolean getUploadSubmitReadyFile() {
    return uploadSubmitReadyFile;
  }

  public void setUploadSubmitReadyFile(boolean uploadSubmitReadyFile) {
    this.uploadSubmitReadyFile = uploadSubmitReadyFile;
  }

  @Override
  public String toString() {
    return "NcbiSubmission{" +
        "id='" + id + '\'' +
        ", cedarUserId='" + cedarUserId + '\'' +
        ", localFilePaths=" + localFilePaths +
        ", submissionFolder='" + submissionFolder + '\'' +
        ", uploadSubmitReadyFile='" + uploadSubmitReadyFile + '\'' +
        '}';
  }
}
