package org.metadatacenter.impex.imp.cadsr;

import java.util.Map;

public class CadsrImportStatus {

  private String uploadId;
  private Map<String, CadsrFileImportStatus> filesImportStatus; // the String stores the file name (e.g., form1.xml)
  private String destinationCedarFolderId;

  public CadsrImportStatus(String uploadId, Map<String, CadsrFileImportStatus> filesImportStatus,
                           String destinationCedarFolderId) {
    this.uploadId = uploadId;
    this.filesImportStatus = filesImportStatus;
    this.destinationCedarFolderId = destinationCedarFolderId;
  }

  public String getUploadId() { return uploadId; }

  public void setUploadId(String uploadId) { this.uploadId = uploadId; }

  public Map<String, CadsrFileImportStatus> getFilesImportStatus() {
    return filesImportStatus;
  }

  public void setFilesImportStatus(Map<String, CadsrFileImportStatus> filesImportStatus) {
    this.filesImportStatus = filesImportStatus;
  }

  public String getDestinationCedarFolderId() {
    return destinationCedarFolderId;
  }

  public void setDestinationCedarFolderId(String destinationCedarFolderId) {
    this.destinationCedarFolderId = destinationCedarFolderId;
  }
}
