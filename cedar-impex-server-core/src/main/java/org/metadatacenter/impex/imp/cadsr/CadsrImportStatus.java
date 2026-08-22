package org.metadatacenter.impex.imp.cadsr;

import java.util.Map;

public class CadsrImportStatus {

  private String uploadId;
  private String ownerUserId; // the user who initiated the import; every read must be filtered by it
  private Map<String, CadsrFileImportStatus> filesImportStatus; // the String stores the file name (e.g., form1.xml)
  private String destinationCedarFolderId;

  public CadsrImportStatus(String uploadId, String ownerUserId, Map<String, CadsrFileImportStatus> filesImportStatus,
                           String destinationCedarFolderId) {
    this.uploadId = uploadId;
    this.ownerUserId = ownerUserId;
    this.filesImportStatus = filesImportStatus;
    this.destinationCedarFolderId = destinationCedarFolderId;
  }

  public String getUploadId() { return uploadId; }

  public void setUploadId(String uploadId) { this.uploadId = uploadId; }

  public String getOwnerUserId() { return ownerUserId; }

  public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }

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
