package org.metadatacenter.impex.upload;

import java.util.Map;

public class UploadStatus {

  private long totalFilesCount;
  private long uploadedFilesCount;
  private Map<String, FileUploadStatus> filesUploadStatus;
  private String uploadLocalPath;

  public UploadStatus(long totalFilesCount, long uploadedFilesCount, Map<String, FileUploadStatus>
      filesUploadStatus, String uploadLocalPath) {
    this.totalFilesCount = totalFilesCount;
    this.uploadedFilesCount = uploadedFilesCount;
    this.filesUploadStatus = filesUploadStatus;
    this.uploadLocalPath = uploadLocalPath;
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

  public String getUploadLocalPath() {
    return uploadLocalPath;
  }

  public void setUploadLocalPath(String uploadLocalPath) {
    this.uploadLocalPath = uploadLocalPath;
  }
}
