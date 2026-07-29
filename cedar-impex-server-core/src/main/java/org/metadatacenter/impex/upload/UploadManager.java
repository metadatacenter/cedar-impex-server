package org.metadatacenter.impex.upload;

import jakarta.ws.rs.BadRequestException;
import org.metadatacenter.impex.exception.UploadInstanceNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadManager {

  private static UploadManager singleInstance;
  // Keyed by (ownerUserId, uploadId), not by the client-supplied uploadId alone. Otherwise two users
  // that pick the same uploadId would share one entry: their chunk counts would merge and
  // getUploadFilePaths would return one user's files to the other, who would then import them under
  // their own credentials. Composing the owner into the key makes another user's entry unaddressable.
  private Map<String, UploadStatus> uploadStatus = new HashMap<>(); // key(ownerUserId, uploadId) -> uploadStatus

  // Single instance
  private UploadManager() {
  }

  public static synchronized UploadManager getInstance() {
    if (singleInstance == null) {
      singleInstance = new UploadManager();
    }
    return singleInstance;
  }

  // A NUL byte cannot appear in a CEDAR user id or in a flow.js uploadId, so it is an unambiguous
  // separator: no (ownerUserId, uploadId) pair can collide with a different pair.
  private static String key(String ownerUserId, String uploadId) {
    return ownerUserId + "\u0000" + uploadId;
  }

  // Updates the upload status with the latest file chunk that has been uploaded
  public synchronized void updateStatus(FlowData data, String ownerUserId, String uploadFolderPath) {

    String key = key(ownerUserId, data.getUploadId());
    String fileId = data.getFlowIdentifier();
    long totalFilesCount = data.getTotalFilesCount();
    long fileTotalChunks = data.getFlowTotalChunks();

    // If the upload does not exist in the map, create it
    if (!uploadStatus.containsKey(key)) {
      Map<String, FileUploadStatus> filesUploadStatus = new HashMap<>();
      UploadStatus status =
          new UploadStatus(totalFilesCount, 0, filesUploadStatus, uploadFolderPath);
      uploadStatus.put(key, status);
    }
    UploadStatus status = uploadStatus.get(key);

    // If the file does not exist in the upload, create it
    if (!status.getFilesUploadStatus().containsKey(fileId)) {
      String fileLocalPath = FlowUploadUtil.getFileLocalFolderPath(uploadFolderPath, data.flowFilename);

      FileUploadStatus fileUploadStatus =
          new FileUploadStatus(fileTotalChunks, 0, fileLocalPath);
      status.getFilesUploadStatus().put(fileId, fileUploadStatus);
    }

    FileUploadStatus fileUploadStatus = status.getFilesUploadStatus().get(fileId);

    // Increase the number of file chunks uploaded
    long uploadedChunks = fileUploadStatus.getFileUploadedChunks();
    fileUploadStatus.setFileUploadedChunks(uploadedChunks + 1);

    // Increase the number of files uploaded, if the chunk was the last one for a file
    if (isFileUploadComplete(fileUploadStatus)) {
      long uploadedFiles = status.getUploadedFilesCount();
      status.setUploadedFilesCount(uploadedFiles + 1);
    }
  }

  private boolean isFileUploadComplete(FileUploadStatus fileUploadStatus) {
    if (fileUploadStatus.getFileUploadedChunks() == fileUploadStatus.getFileTotalChunks()) {
      return true;
    } else if (fileUploadStatus.getFileUploadedChunks() > fileUploadStatus.getFileTotalChunks()) {
      throw new BadRequestException("Uploaded file chunks is higher than total file chunks");
    } else {
      return false;
    }
  }

  public synchronized boolean isUploadComplete(String ownerUserId, String uploadId) throws UploadInstanceNotFoundException {
    String key = key(ownerUserId, uploadId);
    if (!uploadStatus.containsKey(key)) {
      throw new UploadInstanceNotFoundException("Upload not found (uploadId = " + uploadId);
    }
    UploadStatus status = uploadStatus.get(key);

    if (status.getUploadedFilesCount() == status.getTotalFilesCount()) {
      return true;
    } else if (status.getUploadedFilesCount() > status.getTotalFilesCount()) {
      throw new BadRequestException("Number of uploaded files is higher than the total number of files (uploadId = " +
          uploadId);
    } else {
      return false;
    }
  }

  public synchronized void removeUploadStatus(String ownerUserId, String uploadId) {
    uploadStatus.remove(key(ownerUserId, uploadId));
  }

  // Returns local file paths
  public synchronized List<String> getUploadFilePaths(String ownerUserId, String uploadId) throws UploadInstanceNotFoundException {
    String key = key(ownerUserId, uploadId);
    List<String> filePaths = new ArrayList<>();
    if (!uploadStatus.containsKey(key)) {
      throw new UploadInstanceNotFoundException("Upload not found (uploadId = " + uploadId);
    }
    if (!isUploadComplete(ownerUserId, uploadId)) {
      throw new BadRequestException("The upload is not complete (uploadId = " + uploadId);
    }
    UploadStatus status = uploadStatus.get(key);
    for (Map.Entry<String, FileUploadStatus> entry : status.getFilesUploadStatus().entrySet()) {
      filePaths.add(entry.getValue().getFileLocalPath());
    }
    return filePaths;
  }

  public List<String> getUploadFileNames(String ownerUserId, String uploadId) throws UploadInstanceNotFoundException {
    List<String> uploadFileNames = new ArrayList<>();
    List<String> uploadFilePaths = getUploadFilePaths(ownerUserId, uploadId);
    for (String path : uploadFilePaths) {
      uploadFileNames.add(path.substring(path.lastIndexOf("/") + 1));
    }
    return uploadFileNames;
  }

  public synchronized UploadStatus getUploadStatus(String ownerUserId, String uploadId) {
    return uploadStatus.get(key(ownerUserId, uploadId));
  }
}
