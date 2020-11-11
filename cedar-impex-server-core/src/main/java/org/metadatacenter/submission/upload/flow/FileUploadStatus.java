package org.metadatacenter.submission.upload.flow;

public class FileUploadStatus {

  private long fileTotalChunks;
  private long fileUploadedChunks;
  private String fileLocalPath;
  private boolean isMetadataFile;

  public FileUploadStatus(long fileTotalChunks, long fileUploadedChunks, String fileLocalPath, boolean isMetadataFile) {
    this.fileTotalChunks = fileTotalChunks;
    this.fileUploadedChunks = fileUploadedChunks;
    this.fileLocalPath = fileLocalPath;
    this.isMetadataFile = isMetadataFile;
  }

  public long getFileTotalChunks() {
    return fileTotalChunks;
  }

  public void setFileTotalChunks(long fileTotalChunks) {
    this.fileTotalChunks = fileTotalChunks;
  }

  public long getFileUploadedChunks() {
    return fileUploadedChunks;
  }

  public void setFileUploadedChunks(long fileUploadedChunks) {
    this.fileUploadedChunks = fileUploadedChunks;
  }

  public String getFileLocalPath() {
    return fileLocalPath;
  }

  public void setFileLocalPath(String fileLocalPath) {
    this.fileLocalPath = fileLocalPath;
  }

  public boolean isMetadataFile() {
    return isMetadataFile;
  }

  public void setMetadataFile(boolean metadataFile) {
    isMetadataFile = metadataFile;
  }
}
