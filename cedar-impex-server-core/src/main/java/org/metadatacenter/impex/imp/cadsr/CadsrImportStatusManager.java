package org.metadatacenter.impex.imp.cadsr;

import org.metadatacenter.impex.upload.FileUploadStatus;
import org.metadatacenter.impex.upload.UploadManager;
import org.metadatacenter.impex.upload.UploadStatus;

import java.util.HashMap;
import java.util.Map;

public class CadsrImportStatusManager {

  enum ImportStatus {
    PENDING,
    SUCCESS,
    ERROR
  }

  private static CadsrImportStatusManager singleInstance;
  private Map<String, CadsrImportStatus> importStatus = new HashMap<>(); // The String stores the uploadId

  // Single instance
  private CadsrImportStatusManager() {
  }

  public static synchronized CadsrImportStatusManager getInstance() {
    if (singleInstance == null) {
      singleInstance = new CadsrImportStatusManager();
    }
    return singleInstance;
  }

  // Generate import status from upload status
  public synchronized void addStatus(String uploadId, String destinationCedarFolderId) {
    UploadStatus uploadStatus = UploadManager.getInstance().getUploadStatus(uploadId);
    Map<String, CadsrFormImportStatus> formsImportStatus = new HashMap<>();

    for (FileUploadStatus fileUploadStatus : uploadStatus.getFilesUploadStatus().values()) {
      String fileName = fileUploadStatus.getFileLocalPath().substring(fileUploadStatus.getFileLocalPath().lastIndexOf("/") + 1);
      formsImportStatus.put(fileName, new CadsrFormImportStatus(ImportStatus.PENDING));
    }

    importStatus.put(uploadId, new CadsrImportStatus(uploadId, formsImportStatus, destinationCedarFolderId));
  }

  public CadsrImportStatus getStatus(String uploadId) {
    return importStatus.get(uploadId);
  }

//  public synchronized void setStatus(String uploadId, String fileName, ImportStatus status) {
//
//    if (!importStatus.containsKey(uploadId)) {
//
//    }
//    else {
//      CadsrImportStatus s = importStatus.get(uploadId);
//      s.
//    }
//
//
//
//  }



}
