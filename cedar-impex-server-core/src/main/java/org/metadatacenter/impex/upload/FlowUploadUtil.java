package org.metadatacenter.impex.upload;

import jakarta.ws.rs.BadRequestException;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class FlowUploadUtil {

  final static Logger logger = LoggerFactory.getLogger(FlowUploadUtil.class);

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
    DiskFileItem uploadFile = null;
    Map<String, String> additionalParameters = new HashMap<>();

    try {
      for (DiskFileItem item : fileItems) {
        if (item.isFormField()) {
          if (item.getFieldName().equals("uploadId")) {
            uploadId = item.getString();
          } else if (item.getFieldName().equals("numberOfFiles")) {
            numberOfFiles = parseNumber(item.getString());
          } else if (item.getFieldName().equals("flowChunkNumber")) {
            flowChunkNumber = parseNumber(item.getString());
          } else if (item.getFieldName().equals("flowChunkSize")) {
            flowChunkSize = parseNumber(item.getString());
          } else if (item.getFieldName().equals("flowCurrentChunkSize")) {
            flowCurrentChunkSize = parseNumber(item.getString());
          } else if (item.getFieldName().equals("flowTotalSize")) {
            flowTotalSize = parseNumber(item.getString());
          } else if (item.getFieldName().equals("flowIdentifier")) {
            flowIdentifier = item.getString();
          } else if (item.getFieldName().equals("flowFilename")) {
            flowFilename = item.getString();
          } else if (item.getFieldName().equals("flowRelativePath")) {
            flowRelativePath = item.getString();
          } else if (item.getFieldName().equals("flowTotalChunks")) {
            flowTotalChunks = parseNumber(item.getString());
            // Additional parameters
          } else {
            additionalParameters.put(item.getFieldName(), item.getString());
          }
        } else {
          if (uploadFile != null) throw new BadRequestException("Expected one file chunk");
          uploadFile = item;
        }
      }

      // Throw an exception if any of the expected fields is missing
      if (uploadId == null) {
        throw new BadRequestException("Missing field: uploadId");
      } else if (numberOfFiles == -1) {
        throw new BadRequestException("Missing field: numberOfFiles");
      } else if (flowChunkNumber == -1) {
        throw new BadRequestException("Missing field: flowChunkNumber");
      } else if (flowChunkSize == -1) {
        throw new BadRequestException("Missing field: flowChunkSize");
      } else if (flowCurrentChunkSize == -1) {
        throw new BadRequestException("Missing field: flowCurrentChunkSize");
      } else if (flowTotalSize == -1) {
        throw new BadRequestException("Missing field: flowTotalSize");
      } else if (flowIdentifier == null) {
        throw new BadRequestException("Missing field: flowIdentifier");
      } else if (flowFilename == null) {
        throw new BadRequestException("Missing field: flowFilename");
      } else if (flowRelativePath == null) {
        throw new BadRequestException("Missing field: flowRelativePath");
      } else if (flowTotalChunks == -1) {
        throw new BadRequestException("Missing field: flowTotalChunks");
      }

      if (uploadFile == null) throw new BadRequestException("Missing file chunk");
      final DiskFileItem file = uploadFile;
      InputStream flowFileInputStream = new java.io.FilterInputStream(file.getInputStream()) {
        private boolean closed;
        @Override public void close() throws IOException {
          if (!closed) {
            closed = true;
            try { super.close(); } finally { file.delete(); }
          }
        }
      };
      for (DiskFileItem item : fileItems) if (item.isFormField()) item.delete();

      return new FlowData(uploadId, numberOfFiles, flowChunkNumber, flowChunkSize,
          flowCurrentChunkSize,
          flowTotalSize, flowIdentifier, flowFilename, flowRelativePath, flowTotalChunks, flowFileInputStream,
          additionalParameters);
    } catch (RuntimeException | IOException e) {
      for (DiskFileItem item : fileItems) {
        try { item.delete(); } catch (IOException cleanup) { e.addSuppressed(cleanup); }
      }
      throw e;
    }

  }

  /** Multipart Content-Length includes form fields and boundaries; only the chunk's own size is relevant. */
  public static String saveToLocalFile(FlowData data, String userId, int contentLength, String folderPath)
      throws IOException {
    return UploadManager.getInstance().accept(data, userId, folderPath);
  }

  public static String getUploadLocalFolderPath(String baseFolderName, String userId, String uploadId) {
    // userId is server-derived and uploadId is client-supplied; sanitize both so neither can inject a
    // path component (e.g. an uploadId of "../..") and redirect the upload folder outside the tmp root.
    if (!sanitizePathSegment(uploadId).equals(uploadId)) {
      throw new BadRequestException("Upload identifier must be a single path segment");
    }
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
    return org.metadatacenter.util.upload.ChunkUploadStore.basename(raw);
  }

  private static long parseNumber(String value) {
    try { return Long.parseLong(value); }
    catch (NumberFormatException e) { throw new BadRequestException("Invalid numeric upload field", e); }
  }

  public static String getLastFragmentOfUrl(String url) {
    return url.substring(url.lastIndexOf("/") + 1);
  }
}
