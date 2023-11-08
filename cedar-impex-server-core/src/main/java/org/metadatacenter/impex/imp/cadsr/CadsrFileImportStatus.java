package org.metadatacenter.impex.imp.cadsr;

import java.time.LocalTime;

public class CadsrFileImportStatus {

  private String fileName;
  private CadsrImportStatusManager.ImportStatus importStatus;
  private LocalTime statusTime;
  private String report;

  public CadsrFileImportStatus(String fileName, CadsrImportStatusManager.ImportStatus importStatus, LocalTime statusTime, String report) {
    this.fileName = fileName;
    this.importStatus = importStatus;
    this.statusTime = statusTime;
    this.report = report;
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

  public String getReport() {
    return report;
  }

  public void setReport(String report) {
    this.report = report;
  }
}
