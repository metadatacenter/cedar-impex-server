package org.metadatacenter.impex.imp.cadsr;

import org.metadatacenter.impex.upload.FileUploadStatus;
import org.metadatacenter.impex.upload.UploadManager;
import org.metadatacenter.impex.upload.UploadStatus;
import org.metadatacenter.impex.util.ImpexUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CadsrImportStatusManager {

  final static Logger logger = LoggerFactory.getLogger(CadsrImportStatusManager.class);

  public enum ImportStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETE,
    ERROR
  }

  // Imports older than this threshold will be removed from the map
  private final long CLEAN_THRESHOLD_1_MINUTES = 10; // Main threshold, used to clean completed imports
  private final long CLEAN_THRESHOLD_2_MINUTES = 60; // Much longer, to avoid keeping errored imports in the map
  // Frequency used to check if there are any imports that can be removed from the map
  private final long CLEAN_DELAY_MINUTES = 10;

  private static CadsrImportStatusManager singleInstance;
  private Map<String, CadsrImportStatus> importStatus; // uploadId -> CadsrImportStatus
  private ScheduledExecutorService executor;

  // Single instance
  private CadsrImportStatusManager() {
    importStatus = new ConcurrentHashMap<>();
    initUploadsCleanerExecutor();
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
  public void initUploadsCleanerExecutor() {
    executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
      // Daemon so the periodic cleaner thread never keeps the JVM alive during shutdown.
      Thread thread = new Thread(runnable, "cadsr-import-status-cleaner");
      thread.setDaemon(true);
      return thread;
    });
    Runnable cleanTask = () -> {
      logger.info("Checking if there are any old imports that can be removed (map size: " + importStatus.size() + ")");
      List<String> uploadIdsToBeRemoved = new ArrayList<>();
      for (CadsrImportStatus importStatus : importStatus.values()) {
        int countMeetsConditions = 0;
        for (CadsrFileImportStatus fileStatus : importStatus.getFilesImportStatus().values()) {
          // Checks if all the files that belong to the particular upload are older than the given threshold. If any
          // of them is more recent than the threshold, we don't remove the uploadId from the map
          // Elapsed time from a fixed instant, not LocalTime: LocalTime wraps at midnight, so an
          // import finished shortly before midnight would be misjudged as arbitrarily old or young.
          // Completed imports are retained for the short threshold; errored imports for the longer one, so a
          // user has time to read the failure report. Pick the threshold by status instead of OR-ing the two
          // (the old `age >= 10 || age >= 60` always collapsed to `age >= 10`, evicting errors after 10 min).
          long thresholdMinutes;
          if (fileStatus.getImportStatus() == ImportStatus.COMPLETE) {
            thresholdMinutes = CLEAN_THRESHOLD_1_MINUTES;
          } else if (fileStatus.getImportStatus() == ImportStatus.ERROR) {
            thresholdMinutes = CLEAN_THRESHOLD_2_MINUTES;
          } else {
            break; // still PENDING or IN_PROGRESS: keep the whole upload
          }
          long ageMinutes = Duration.between(fileStatus.getStatusTime(), Instant.now()).toMinutes();
          if (ageMinutes >= thresholdMinutes) {
            countMeetsConditions++;
          }
          else {
            break;
          }
        }
        // Remove from map if all the files are older than one of the thresholds
        if (countMeetsConditions == importStatus.getFilesImportStatus().size()) {
          uploadIdsToBeRemoved.add(importStatus.getUploadId());
        }
      }
      for (String uploadId : uploadIdsToBeRemoved) {
        if (importStatus.containsKey(uploadId)) {
          importStatus.remove(uploadId);
          logger.info("UploadId removed: " + uploadId + ". Updated map size: " + importStatus.size());
        }
      }
    };
    executor.scheduleWithFixedDelay(cleanTask, 0, CLEAN_DELAY_MINUTES, TimeUnit.MINUTES);
  }

  public CadsrImportStatus getStatus(String uploadId) {
    return importStatus.get(uploadId);
  }

  /**
   * The status for an upload, but only if it belongs to {@code userId}; null otherwise, so a caller can
   * neither read nor even confirm the existence of another user's import.
   */
  public CadsrImportStatus getStatusForUser(String uploadId, String userId) {
    CadsrImportStatus status = importStatus.get(uploadId);
    if (status != null && userId != null && userId.equals(status.getOwnerUserId())) {
      return status;
    }
    return null;
  }

  /**
   * An immutable snapshot of only the given user's imports. This replaces the former getAllStatuses(),
   * which returned every user's imports (filenames, reports, destination folder ids) to any caller.
   */
  public Map<String, CadsrImportStatus> getStatusesForUser(String userId) {
    Map<String, CadsrImportStatus> mine = new HashMap<>();
    for (Map.Entry<String, CadsrImportStatus> entry : importStatus.entrySet()) {
      if (userId != null && userId.equals(entry.getValue().getOwnerUserId())) {
        mine.put(entry.getKey(), entry.getValue());
      }
    }
    return Map.copyOf(mine);
  }

  /**
   * Adds the upload information to the map and sets the import status to PENDING for all the forms
   * @param uploadId
   */
  public synchronized void initImportStatus(String uploadId, String ownerUserId, String destinationCedarFolderId) {
    // Get the file names from the UploadManager
    UploadStatus uploadStatus = UploadManager.getInstance().getUploadStatus(ownerUserId, uploadId);
    Map<String, CadsrFileImportStatus> filesImportStatus = new HashMap<>();
    for (FileUploadStatus fileUploadStatus : uploadStatus.getFilesUploadStatus().values()) {
      String fileName = ImpexUtil.getFileNameFromFilePath(fileUploadStatus.getFileLocalPath());
      filesImportStatus.put(fileName, new CadsrFileImportStatus(fileName, ImportStatus.PENDING, Instant.now(), ""));
    }
    importStatus.put(uploadId, new CadsrImportStatus(uploadId, ownerUserId, filesImportStatus, destinationCedarFolderId));
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
    if (!importStatus.get(uploadId).getFilesImportStatus().containsKey(fileName)) {
      throw new IllegalArgumentException("fileName not found: " + fileName);
    }

    CadsrFileImportStatus fis = importStatus.get(uploadId).getFilesImportStatus().get(fileName);
    fis.setImportStatus(status);
    // Cleanup age is measured from the latest status change. Without this, a file keeps its PENDING
    // timestamp, so a long-running import could be swept the moment it reached COMPLETE.
    fis.setStatusTime(Instant.now());

    Map<String, CadsrFileImportStatus> fisMap = importStatus.get(uploadId).getFilesImportStatus();
    fisMap.put(fileName, fis);

    CadsrImportStatus is = importStatus.get(uploadId);
    is.setFilesImportStatus(fisMap);

    importStatus.replace(uploadId, is);

  }

  private synchronized void removeImportStatus(String uploadId) {
    importStatus.remove(uploadId);
  }

  /**
   * Stops the periodic cleaner. Wired into the server lifecycle so the scheduled executor is released on
   * shutdown; harmless if never called, since the cleaner thread is a daemon.
   */
  public synchronized void shutdown() {
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  /** Stops the cleaner only if the singleton was ever created, so it isn't instantiated just to shut it down. */
  public static synchronized void shutdownIfRunning() {
    if (singleInstance != null) {
      singleInstance.shutdown();
    }
  }

  public boolean exists(String uploadId) {
    return importStatus.containsKey(uploadId);
  }

  /**
   * Writes multiple lines to the report
   * @param uploadId
   * @param fileName
   * @param messages
   */
  public void writeReportMessages(String uploadId, String fileName, List<String> messages) {
    for (String message : messages) {
      writeReportMessage(uploadId, fileName, message);
    }
  }

  /**
   * Writes a new line to the report
   * @param uploadId
   * @param fileName
   * @param message
   */
  public void writeReportMessage(String uploadId, String fileName, String message) {
    writeReportMessage(uploadId, fileName, message, false);
  }

  public synchronized void writeReportMessage(String uploadId, String fileName, String message, boolean includeDateTime) {

    if (importStatus.containsKey(uploadId)) {

      if (importStatus.get(uploadId).getFilesImportStatus().containsKey(fileName)) {
        String dateTime = "";
        if (includeDateTime) {
          DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
          dateTime = "[" + dtf.format(LocalDateTime.now()) + "] ";
        }
        String currentReport = importStatus.get(uploadId).getFilesImportStatus().get(fileName).getReport();
        String lineSeparator = currentReport.length() > 0 ? System.getProperty("line.separator") : "";
        String newReport = currentReport + lineSeparator + dateTime + message;

        CadsrFileImportStatus fis = importStatus.get(uploadId).getFilesImportStatus().get(fileName);
        fis.setReport(newReport);

        Map<String, CadsrFileImportStatus> fisMap = importStatus.get(uploadId).getFilesImportStatus();
        fisMap.replace(fileName, fis);

        CadsrImportStatus is = importStatus.get(uploadId);
        is.setFilesImportStatus(fisMap);

        importStatus.replace(uploadId, is);
      }
      else {
        throw new IllegalArgumentException("FileName not found: " + fileName);
      }
    }
    else {
      throw new IllegalArgumentException("UploadId not found: " + uploadId);
    }
  }

}
