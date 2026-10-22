package org.metadatacenter.impex.imp.cadsr;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "How the import of one upload is going, file by file.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
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

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Identifies the upload, as its chunks named it.")
  public String getUploadId() { return uploadId; }

  public void setUploadId(String uploadId) { this.uploadId = uploadId; }

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The user who ran the import. No other user is shown it.")
  public String getOwnerUserId() { return ownerUserId; }

  public void setOwnerUserId(String ownerUserId) { this.ownerUserId = ownerUserId; }

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Each file's import, keyed by file name.")
  public Map<String, CadsrFileImportStatus> getFilesImportStatus() {
    return filesImportStatus;
  }

  public void setFilesImportStatus(Map<String, CadsrFileImportStatus> filesImportStatus) {
    this.filesImportStatus = filesImportStatus;
  }

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
      description = "The folder the templates are created in. It is null when the caller named no folder "
          + "and has no home folder yet.")
  public String getDestinationCedarFolderId() {
    return destinationCedarFolderId;
  }

  public void setDestinationCedarFolderId(String destinationCedarFolderId) {
    this.destinationCedarFolderId = destinationCedarFolderId;
  }
}
