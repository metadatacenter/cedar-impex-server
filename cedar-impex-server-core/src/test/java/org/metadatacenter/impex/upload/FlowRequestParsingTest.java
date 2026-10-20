package org.metadatacenter.impex.upload;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Each request of a chunked upload is a multipart form carrying the upload's identity, the chunk's position and
 * size, the file's identity, and the chunk itself. The server reads every field before storing anything, and refuses
 * a request that lacks one, carries a malformed number, or carries other than one chunk, naming the problem.
 */
class FlowRequestParsingTest {

  private static Map<String, String> fields() {
    Map<String, String> fields = new LinkedHashMap<>();
    fields.put("uploadId", "upload-1");
    fields.put("numberOfFiles", "2");
    fields.put("flowChunkNumber", "1");
    fields.put("flowChunkSize", "4");
    fields.put("flowCurrentChunkSize", "4");
    fields.put("flowTotalSize", "8");
    fields.put("flowIdentifier", "file-1");
    fields.put("flowFilename", "form.xml");
    fields.put("flowRelativePath", "forms/form.xml");
    fields.put("flowTotalChunks", "2");
    return fields;
  }

  @Test
  void everyFieldAndTheChunkAreRead() throws Exception {
    Map<String, String> fields = fields();
    fields.put("comment", "kept aside");

    try (FlowData data = FlowUploadUtil.getFlowData(request(fields, 1))) {
      assertEquals("upload-1", data.getUploadId());
      assertEquals(2, data.getTotalFilesCount());
      assertEquals(1, data.getFlowChunkNumber());
      assertEquals(4, data.getFlowChunkSize());
      assertEquals(4, data.getFlowCurrentChunkSize());
      assertEquals(8, data.getFlowTotalSize());
      assertEquals("file-1", data.getFlowIdentifier());
      assertEquals("form.xml", data.getFlowFilename());
      assertEquals("forms/form.xml", data.getFlowRelativePath());
      assertEquals(2, data.getFlowTotalChunks());
      assertEquals(Map.of("comment", "kept aside"), data.getAdditionalParameters());
      assertEquals("abcd", new String(data.getFlowFileInputStream().readAllBytes(), StandardCharsets.UTF_8));
    }
  }

  @Test
  void aMissingFieldIsNamed() {
    for (String field : fields().keySet()) {
      Map<String, String> fields = fields();
      fields.remove(field);
      BadRequestException refusal = assertThrows(BadRequestException.class,
          () -> FlowUploadUtil.getFlowData(request(fields, 1)), field);
      assertEquals("Missing field: " + field, refusal.getMessage());
    }
  }

  @Test
  void aMalformedNumberOrAWrongChunkCountIsRefused() {
    Map<String, String> fields = fields();
    fields.put("flowChunkNumber", "first");
    assertEquals("Invalid numeric upload field",
        assertThrows(BadRequestException.class, () -> FlowUploadUtil.getFlowData(request(fields, 1))).getMessage());
    assertEquals("Missing file chunk",
        assertThrows(BadRequestException.class, () -> FlowUploadUtil.getFlowData(request(fields(), 0))).getMessage());
    assertEquals("Expected one file chunk",
        assertThrows(BadRequestException.class, () -> FlowUploadUtil.getFlowData(request(fields(), 2))).getMessage());
  }

  @Test
  void anUploadsFolderIsNamedByItsOwnerAndIdentifier() {
    String folder = FlowUploadUtil.getUploadLocalFolderPath("impex-upload", "https://metadatacenter.org/users/u-1",
        "upload-1");
    assertEquals(System.getProperty("java.io.tmpdir") + "/impex-upload/user_u-1/upload_upload-1", folder);
    assertEquals(folder + "/form.xml", FlowUploadUtil.getFileLocalFolderPath(folder, "../form.xml"),
        "a file name cannot climb out of its folder");
    assertEquals("u-1", FlowUploadUtil.getLastFragmentOfUrl("https://metadatacenter.org/users/u-1"));
  }

  /** A servlet request whose body is a multipart form of the given fields and as many chunks as given. */
  private static HttpServletRequest request(Map<String, String> fields, int chunks) {
    StringBuilder body = new StringBuilder();
    fields.forEach((name, value) -> body.append("--b\r\nContent-Disposition: form-data; name=\"").append(name)
        .append("\"\r\n\r\n").append(value).append("\r\n"));
    for (int i = 0; i < chunks; i++) {
      body.append("--b\r\nContent-Disposition: form-data; name=\"file\"; filename=\"form.xml\"\r\n")
          .append("Content-Type: application/octet-stream\r\n\r\nabcd\r\n");
    }
    body.append("--b--\r\n");
    byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
    InputStream in = new ByteArrayInputStream(bytes);
    ServletInputStream servletIn = new ServletInputStream() {
      @Override
      public int read() throws java.io.IOException {
        return in.read();
      }

      @Override
      public int read(byte[] b, int off, int len) throws java.io.IOException {
        return in.read(b, off, len);
      }

      @Override
      public boolean isFinished() {
        return false;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener listener) {
      }
    };
    return (HttpServletRequest) Proxy.newProxyInstance(FlowRequestParsingTest.class.getClassLoader(),
        new Class<?>[]{HttpServletRequest.class}, (proxy, method, args) -> switch (method.getName()) {
          case "getContentType" -> "multipart/form-data; boundary=b";
          case "getMethod" -> "POST";
          case "getInputStream" -> servletIn;
          case "getContentLength" -> bytes.length;
          case "getContentLengthLong" -> (long) bytes.length;
          case "getCharacterEncoding" -> "UTF-8";
          case "getHeader" -> null;
          default -> null;
        });
  }
}
