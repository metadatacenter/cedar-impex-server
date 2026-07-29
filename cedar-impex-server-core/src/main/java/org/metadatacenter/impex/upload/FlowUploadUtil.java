package org.metadatacenter.impex.upload;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet5.JakartaServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.*;

public class FlowUploadUtil {

  final static Logger logger = LoggerFactory.getLogger(FlowUploadUtil.class);

  // Ceiling on a single assembled upload. The chunk offset is derived from client-supplied values, so
  // without a cap a client could seek far into a file and create a huge sparse file, exhausting the disk.
  private static final long MAX_UPLOAD_BYTES = 5L * 1024 * 1024 * 1024; // 5 GiB

  public static FlowData getFlowData(HttpServletRequest request)
      throws IllegalAccessException, FileUploadException, IOException {

    // Extract all the files or form items that were received within the multipart/form-data POST request
    List<DiskFileItem> fileItems =
        new JakartaServletFileUpload<>(DiskFileItemFactory.builder().get()).parseRequest(request);

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

    for (DiskFileItem item : fileItems) {
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
          logger.error("Error opening the input stream of uploaded file chunk: " + item.getName(), e);
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
    // Containment check on top of the basename sanitization in getFileLocalFolderPath: the resolved
    // target must stay under the upload root, so a crafted filename can never write outside it.
    Path uploadRoot = new File(folderPath).toPath().toAbsolutePath().normalize();
    Path target = new File(fileLocalFolderPath).toPath().toAbsolutePath().normalize();
    if (!target.startsWith(uploadRoot)) {
      throw new IOException("Resolved upload path escapes the upload root: " + target);
    }
    File file = target.toFile();
    if (!file.getParentFile().exists()) {
      file.getParentFile().mkdirs();
    }
    if (!file.exists()) {
      file.createNewFile();
    }
    // Use a random access file to assemble all the file chunks
    try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
      FlowUploadUtil.writeToRandomAccessFile(raf, data, contentLength);
    }
    return file.getAbsolutePath();
  }

  public static void writeToRandomAccessFile(RandomAccessFile raf, FlowData data, long contentLength) throws
      IOException {
    // The chunk offset comes from client-supplied values; validate before trusting it as a file
    // offset, or a client could seek arbitrarily and create a huge sparse file.
    if (data.flowChunkNumber < 1 || data.flowChunkSize < 0 || contentLength < 0) {
      throw new IOException("Invalid chunk coordinates");
    }
    if (data.flowTotalSize < 0 || data.flowTotalSize > MAX_UPLOAD_BYTES) {
      throw new IOException("Declared total upload size exceeds the allowed maximum");
    }
    // multiplyExact/addExact turn a wrap-around into an exception instead of a value that would slip
    // past the size check as a negative number; the subtraction form avoids overflow entirely.
    long offset;
    try {
      offset = Math.multiplyExact(data.flowChunkNumber - 1, data.flowChunkSize);
    } catch (ArithmeticException e) {
      throw new IOException("Chunk offset overflow", e);
    }
    if (offset < 0 || offset > MAX_UPLOAD_BYTES || contentLength > MAX_UPLOAD_BYTES - offset) {
      throw new IOException("Upload offset/size exceeds the allowed maximum");
    }
    raf.seek(offset);
    // The caller owns raf (try-with-resources); here we only need to close the chunk input stream.
    try (InputStream is = data.getFlowFileInputStream()) {
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
    }
  }

  public static String getUploadLocalFolderPath(String baseFolderName, String userId, String uploadId) {
    // userId is server-derived and uploadId is client-supplied; sanitize both so neither can inject a
    // path component (e.g. an uploadId of "../..") and redirect the upload folder outside the tmp root.
    String userFolder = sanitizePathSegment(FlowUploadUtil.getLastFragmentOfUrl(userId));
    return System.getProperty("java.io.tmpdir") + "/" + baseFolderName + "/user_" + userFolder + "/upload_" +
        sanitizePathSegment(uploadId);
  }

  public static String getFileLocalFolderPath(String uploadLocalFolderPath, String fileName) {
    return uploadLocalFolderPath + "/" + sanitizePathSegment(fileName);
  }

  /**
   * Reduce a client-supplied value to a single safe path segment: strip any directory components (both
   * separators) and reject empty, "." and ".." so it cannot escape its parent directory.
   */
  public static String sanitizePathSegment(String raw) {
    if (raw == null) {
      throw new IllegalArgumentException("Missing path segment");
    }
    String name = raw.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1);
    if (name.isEmpty() || name.equals(".") || name.equals("..")) {
      throw new IllegalArgumentException("Illegal path segment: " + raw);
    }
    return name;
  }

  public static String getLastFragmentOfUrl(String url) {
    return url.substring(url.lastIndexOf("/") + 1);
  }
}
