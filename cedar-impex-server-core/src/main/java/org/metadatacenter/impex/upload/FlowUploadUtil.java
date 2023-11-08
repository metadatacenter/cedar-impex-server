package org.metadatacenter.impex.upload;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;

public class FlowUploadUtil {

  final static Logger logger = LoggerFactory.getLogger(FlowUploadUtil.class);

  public static FlowData getFlowData(HttpServletRequest request) throws IllegalAccessException, FileUploadException {

    // Extract all the files or form items that were received within the multipart/form-data POST request
    List<FileItem> fileItems = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);

    String uploadId = null;
    long numberOfFiles = -1;
    long flowChunkNumber = -1;
    long flowChunkSize = -1;
    long flowCurrentChunkSize = -1;
    long flowTotalSize = -1;
    String flowIdentifier = null;
    String flowFilename = null;
    String flowRelativePath = null;
    long flowTotalChunks = -1;
    InputStream flowFileInputStream = null;
    Map<String, String> additionalParameters = new HashMap<>();

    for (FileItem item : fileItems) {
      if (item.isFormField()) {
        if (item.getFieldName().equals("uploadId")) {
          uploadId = item.getString();
        } else if (item.getFieldName().equals("numberOfFiles")) {
          numberOfFiles = Long.parseLong(item.getString());
        } else if (item.getFieldName().equals("flowChunkNumber")) {
          flowChunkNumber = Long.parseLong(item.getString());
        } else if (item.getFieldName().equals("flowChunkSize")) {
          flowChunkSize = Long.parseLong(item.getString());
        } else if (item.getFieldName().equals("flowCurrentChunkSize")) {
          flowCurrentChunkSize = Long.parseLong(item.getString());
        } else if (item.getFieldName().equals("flowTotalSize")) {
          flowTotalSize = Long.parseLong(item.getString());
        } else if (item.getFieldName().equals("flowIdentifier")) {
          flowIdentifier = item.getString();
        } else if (item.getFieldName().equals("flowFilename")) {
          flowFilename = item.getString();
        } else if (item.getFieldName().equals("flowRelativePath")) {
          flowRelativePath = item.getString();
        } else if (item.getFieldName().equals("flowTotalChunks")) {
          flowTotalChunks = Long.parseLong(item.getString());
          // Additional parameters
        } else {
          additionalParameters.put(item.getFieldName(), item.getString());
        }
      } else { // It is a file
        try {
          flowFileInputStream = item.getInputStream();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    // Throw an exception if any of the expected fields is missing
    if (uploadId == null) {
      throw new InternalError("Missing field: uploadId");
    } else if (numberOfFiles == -1) {
      throw new InternalError("Missing field: numberOfFiles");
    } else if (flowChunkNumber == -1) {
      throw new InternalError("Missing field: flowChunkNumber");
    } else if (flowChunkSize == -1) {
      throw new InternalError("Missing field: flowChunkSize");
    } else if (flowCurrentChunkSize == -1) {
      throw new InternalError("Missing field: flowCurrentChunkSize");
    } else if (flowTotalSize == -1) {
      throw new InternalError("Missing field: flowTotalSize");
    } else if (flowIdentifier == null) {
      throw new InternalError("Missing field: flowIdentifier");
    } else if (flowFilename == null) {
      throw new InternalError("Missing field: flowFilename");
    } else if (flowRelativePath == null) {
      throw new InternalError("Missing field: flowRelativePath");
    } else if (flowTotalChunks == -1) {
      throw new InternalError("Missing field: flowTotalChunks");
    }

    return new FlowData(uploadId, numberOfFiles, flowChunkNumber, flowChunkSize,
        flowCurrentChunkSize,
        flowTotalSize, flowIdentifier, flowFilename, flowRelativePath, flowTotalChunks, flowFileInputStream,
        additionalParameters);

  }

  public static String saveToLocalFile(FlowData data, String userId, int contentLength, String folderPath) throws
      IOException {

    String fileLocalFolderPath = FlowUploadUtil.getFileLocalFolderPath(folderPath, data.flowFilename);
    File file = new File(fileLocalFolderPath);
    //logger.info("Local file path: " + fileLocalFolderPath);
    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }
    if (!file.exists()) {
      file.createNewFile();
    }
    // Use a random access file to assemble all the file chunks
    RandomAccessFile raf = new RandomAccessFile(file, "rw");
    FlowUploadUtil.writeToRandomAccessFile(raf, data, contentLength);
    return file.getAbsolutePath();
  }

  public static void writeToRandomAccessFile(RandomAccessFile raf, FlowData data, long contentLength) throws
      IOException {
    // Seek to position
    raf.seek((data.flowChunkNumber - 1) * data.flowChunkSize);
    // Save to file
    InputStream is = data.getFlowFileInputStream();
    long read = 0;
    byte[] bytes = new byte[1024 * 100];
    while (read < contentLength) {
      int r = is.read(bytes);
      if (r < 0) {
        break;
      }
      raf.write(bytes, 0, r);
      read += r;
    }
    raf.close();
  }

  public static String getUploadLocalFolderPath(String baseFolderName, String userId, String uploadId) {
    String userFolder = FlowUploadUtil.getLastFragmentOfUrl(userId);
    return System.getProperty("java.io.tmpdir") + "/" + baseFolderName + "/user_" + userFolder + "/upload_" +
        uploadId;
  }

  public static String getFileLocalFolderPath(String uploadLocalFolderPath, String fileName) {
    return uploadLocalFolderPath + "/" + fileName;
  }

  public static String getLastFragmentOfUrl(String url) {
    return url.substring(url.lastIndexOf("/") + 1);
  }
}
