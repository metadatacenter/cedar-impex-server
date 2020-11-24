package org.metadatacenter.impex.upload;

public class FileUploadStatus {

  private long fileTotalChunks;
  private long fileUploadedChunks;
  private String fileLocalPath;

  public FileUploadStatus(long fileTotalChunks, long fileUploadedChunks, String fileLocalPath) {
    this.fileTotalChunks = fileTotalChunks;
    this.fileUploadedChunks = fileUploadedChunks;
    this.fileLocalPath = fileLocalPath;
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

}
