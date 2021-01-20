package org.metadatacenter.impex.imp.cadsr;

import java.time.LocalTime;
import java.util.Date;

public class CadsrFileImportStatus {

  private String fileName;
  private CadsrImportStatusManager.ImportStatus importStatus;
  private LocalTime statusTime;

  public CadsrFileImportStatus(String fileName, CadsrImportStatusManager.ImportStatus importStatus, LocalTime statusTime) {
    this.fileName = fileName;
    this.importStatus = importStatus;
    this.statusTime = statusTime;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public CadsrImportStatusManager.ImportStatus getImportStatus() {
    return importStatus;
  }

  public void setImportStatus(CadsrImportStatusManager.ImportStatus importStatus) {
    this.importStatus = importStatus;
  }

  public LocalTime getStatusTime() {
    return statusTime;
  }

  public void setStatusTime(LocalTime statusTime) {
    this.statusTime = statusTime;
  }
}
