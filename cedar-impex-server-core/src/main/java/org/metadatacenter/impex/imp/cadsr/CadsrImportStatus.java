package org.metadatacenter.impex.imp.cadsr;

import java.util.Map;

public class CadsrImportStatus {

  private String uploadId;
  private Map<String, CadsrFormImportStatus> formsImportStatus; // the String stores the file name (e.g., form1.xml)
  private String destinationCedarFolderId;

  public CadsrImportStatus(String uploadId, Map<String, CadsrFormImportStatus> formsImportStatus,
                           String destinationCedarFolderId) {
    this.uploadId = uploadId;
    this.formsImportStatus = formsImportStatus;
    this.destinationCedarFolderId = destinationCedarFolderId;
  }

  public String getUploadId() { return uploadId; }

  public void setUploadId(String uploadId) { this.uploadId = uploadId; }

  public Map<String, CadsrFormImportStatus> getFormsImportStatus() {
    return formsImportStatus;
  }

  public void setFormsImportStatus(Map<String, CadsrFormImportStatus> formsImportStatus) {
    this.formsImportStatus = formsImportStatus;
  }

  public String getDestinationCedarFolderId() {
    return destinationCedarFolderId;
  }

  public void setDestinationCedarFolderId(String destinationCedarFolderId) {
    this.destinationCedarFolderId = destinationCedarFolderId;
  }
}
