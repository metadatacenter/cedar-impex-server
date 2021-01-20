package org.metadatacenter.impex.imp.cadsr;

import org.metadatacenter.impex.upload.FileUploadStatus;
import org.metadatacenter.impex.upload.UploadManager;
import org.metadatacenter.impex.upload.UploadStatus;
import org.metadatacenter.impex.util.ImpexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CadsrImportStatusManager {

  final static Logger logger = LoggerFactory.getLogger(CadsrImportStatusManager.class);

  public enum ImportStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETE
  }

  // Imports older than this threshold will be removed from the map
  private final long REMOVE_OLD_UPLOADS_THRESHOLD_MINUTES = 10;
  // Frequency to check if there are any imports that can be removed from the map
  private final long REMOVE_OLD_UPLOADS_PERIOD_MINUTES = 15;

  private static CadsrImportStatusManager singleInstance;
  private Map<String, CadsrImportStatus> importStatus = new HashMap<>(); // uploadId -> CadsrImportStatus

  // Single instance
  private CadsrImportStatusManager() {
    initUploadsCleaner();
  }

  public static synchronized CadsrImportStatusManager getInstance() {
    if (singleInstance == null) {
      singleInstance = new CadsrImportStatusManager();
    }
    return singleInstance;
  }

  /**
   * Uses an executor to clear the older items in the importStatus map periodically
   */
  public void initUploadsCleaner() {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    Runnable clearTask = () -> {
      logger.info("Checking if there are any old imports that can be removed (map size: " + importStatus.size() + ")");
      for (CadsrImportStatus importStatus : importStatus.values()) {
        boolean removeFromMap = true;
        for (CadsrFileImportStatus fileStatus : importStatus.getFilesImportStatus().values()) {
          // Checks if all the files that belong to the particular upload are older than the given threshold. If any
          // of them is more recent than the threshold, we don't remove the uploadId from the map
          if (fileStatus.getImportStatus() != ImportStatus.COMPLETE ||
              fileStatus.getStatusTime().plusMinutes(REMOVE_OLD_UPLOADS_THRESHOLD_MINUTES).isAfter(LocalTime.now())) {
            removeFromMap = false;
            break;
          }
        }
        if (removeFromMap) {
          logger.info("Removing old upload task from map:  " + importStatus.getUploadId());
          removeImportStatus(importStatus.getUploadId());
        }
      }
    };
    executor.scheduleAtFixedRate(clearTask, REMOVE_OLD_UPLOADS_PERIOD_MINUTES, REMOVE_OLD_UPLOADS_PERIOD_MINUTES, TimeUnit.MINUTES);
  }

  public CadsrImportStatus getStatus(String uploadId) {
    return importStatus.get(uploadId);
  }

  /**
   * Adds the upload information to the map and sets the import status to PENDING for all the forms
   * @param uploadId
   */
  public synchronized void initImportStatus(String uploadId, String destinationCedarFolderId) {
    // Get the file names from the UploadManager
    UploadStatus uploadStatus = UploadManager.getInstance().getUploadStatus(uploadId);
    Map<String, CadsrFileImportStatus> filesImportStatus = new HashMap<>();
    for (FileUploadStatus fileUploadStatus : uploadStatus.getFilesUploadStatus().values()) {
      String fileName = ImpexUtil.getFileNameFromFilePath(fileUploadStatus.getFileLocalPath());
      filesImportStatus.put(fileName, new CadsrFileImportStatus(fileName, ImportStatus.PENDING, LocalTime.now()));
    }
    importStatus.put(uploadId, new CadsrImportStatus(uploadId, filesImportStatus, destinationCedarFolderId));
  }

  /**
   * Set the import status for a particular file, associated to a given uploadId. This method can only be used to set
   * the status to IN_PROGRESS or to COMPLETE. In order to initialize the status to PENDING, use initImportStatus.
   * @param uploadId
   * @param fileName
   * @param status
   */
  public synchronized void setStatus(String uploadId, String fileName, ImportStatus status) {
    if (!importStatus.containsKey(uploadId)) {
      throw new IllegalArgumentException("uploadId not found: " + uploadId);
    }
    if (status != ImportStatus.IN_PROGRESS && status != ImportStatus.COMPLETE) {
      throw new IllegalArgumentException("Invalid import status: " + status);
    }
    importStatus.get(uploadId).getFilesImportStatus().put(fileName, new CadsrFileImportStatus(fileName, status, LocalTime.now()));
  }

  private synchronized void removeImportStatus(String uploadId) {
    importStatus.remove(uploadId);
  }

  public boolean exists(String uploadId) {
    return importStatus.containsKey(uploadId);
  }

}
