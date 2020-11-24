package org.metadatacenter.impex.upload;

import org.metadatacenter.impex.exception.UploadInstanceNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadManager {

  private static UploadManager singleInstance;
  private Map<String, UploadStatus> uploadStatus = new HashMap<>(); // uploadId -> uploadStatus

  // Single instance
  private UploadManager() {
  }

  public static synchronized UploadManager getInstance() {
    if (singleInstance == null) {
      singleInstance = new UploadManager();
    }
    return singleInstance;
  }

  // Updates the upload status with the latest file chunk that has been uploaded
  public synchronized void updateStatus(FlowData data, String uploadFolderPath) {

    String uploadId = data.getUploadId();
    String fileId = data.getFlowIdentifier();
    long totalFilesCount = data.getTotalFilesCount();
    long fileTotalChunks = data.getFlowTotalChunks();

    // If the upload does not exist in the map, create it
    if (!uploadStatus.containsKey(uploadId)) {
      Map<String, FileUploadStatus> filesUploadStatus = new HashMap<>();
      UploadStatus status =
          new UploadStatus(totalFilesCount, 0, filesUploadStatus, uploadFolderPath);
      uploadStatus.put(uploadId, status);
    }
    UploadStatus status = uploadStatus.get(uploadId);

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
      throw new InternalError("Uploaded file chunks is higher than total file chunks");
    } else {
      return false;
    }
  }

  public boolean isUploadComplete(String uploadId) throws UploadInstanceNotFoundException {
    if (!uploadStatus.containsKey(uploadId)) {
      throw new UploadInstanceNotFoundException("Upload not found (uploadId = " + uploadId);
    }
    UploadStatus status = uploadStatus.get(uploadId);

    if (status.getUploadedFilesCount() == status.getTotalFilesCount()) {
      return true;
    } else if (status.getUploadedFilesCount() > status.getTotalFilesCount()) {
      throw new InternalError("Number of uploaded files is higher than the total number of files (uploadId = " +
          uploadId);
    } else {
      return false;
    }
  }

  public void removeUploadStatus(String uploadId) {
    uploadStatus.remove(uploadId);
  }

  // Returns local file paths
  public List<String> getUploadFilePaths(String uploadId) throws UploadInstanceNotFoundException {
    List<String> filePaths = new ArrayList<>();
    if (!uploadStatus.containsKey(uploadId)) {
      throw new UploadInstanceNotFoundException("Upload not found (uploadId = " + uploadId);
    }
    if (!isUploadComplete(uploadId)) {
      throw new InternalError("The upload is not complete (uploadId = " + uploadId);
    }
    UploadStatus status = uploadStatus.get(uploadId);
    for (Map.Entry<String, FileUploadStatus> entry : status.getFilesUploadStatus().entrySet()) {
      filePaths.add(entry.getValue().getFileLocalPath());
    }
    return filePaths;
  }

  public List<String> getUploadFileNames(String uploadId) throws UploadInstanceNotFoundException {
    List<String> uploadFileNames = new ArrayList<>();
    List<String> uploadFilePaths = getUploadFilePaths(uploadId);
    for (String path : uploadFilePaths) {
      uploadFileNames.add(path.substring(path.lastIndexOf("/") + 1));
    }
    return uploadFileNames;
  }

  public UploadStatus getUploadStatus(String uploadId) {
    return uploadStatus.get(uploadId);
  }
}
