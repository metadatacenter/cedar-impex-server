package org.metadatacenter.impex.imp.cadsr;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "How the import of one uploaded file is going.",
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public class CadsrFileImportStatus {

  private String fileName;
  private CadsrImportStatusManager.ImportStatus importStatus;
  private Instant statusTime;
  private String report;

  public CadsrFileImportStatus(String fileName, CadsrImportStatusManager.ImportStatus importStatus, Instant statusTime, String report) {
    this.fileName = fileName;
    this.importStatus = importStatus;
    this.statusTime = statusTime;
    this.report = report;
  }

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The file's name, without any directory part.")
  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
      description = "Where the file's import stands: waiting to start, being translated, saved as a "
          + "template, or failed.")
  public CadsrImportStatusManager.ImportStatus getImportStatus() {
    return importStatus;
  }

  public void setImportStatus(CadsrImportStatusManager.ImportStatus importStatus) {
    this.importStatus = importStatus;
  }

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
      description = "When the status last changed.")
  public Instant getStatusTime() {
    return statusTime;
  }

  public void setStatusTime(Instant statusTime) {
    this.statusTime = statusTime;
  }

  @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
      description = "The messages the translation produced, separated by line breaks. Empty until "
          + "there are any.")
  public String getReport() {
    return report;
  }

  public void setReport(String report) {
    this.report = report;
  }
}
