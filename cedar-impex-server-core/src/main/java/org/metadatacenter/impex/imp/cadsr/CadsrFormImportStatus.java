package org.metadatacenter.impex.imp.cadsr;

import org.metadatacenter.impex.imp.cadsr.CadsrImportStatusManager.ImportStatus;

public class CadsrFormImportStatus {

  private ImportStatus status;
  private String statusMessage;

  public CadsrFormImportStatus(ImportStatus status) {
    this.status = status;
  }

  public ImportStatus getStatus() {
    return status;
  }

  public void setStatus(ImportStatus status) {
    this.status = status;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }
}
