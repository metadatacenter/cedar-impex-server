package org.metadatacenter.impex;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.impex.resources.ImpexServerResource;
import org.metadatacenter.util.test.RouteSurface;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.test.TestAuthUtil;
import org.metadatacenter.impex.upload.FlowUploadUtil;
import org.metadatacenter.impex.upload.UploadManager;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;

/** Real multipart requests exercise authentication, parsing, shared assembly and HTTP error mapping. */
public class ImpexUploadHttpTest {

  static {
    // Must run before the test support boots the server, which reads the port env vars.
    // OS-assigned ports keep concurrent test processes isolated.
    Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
    environment.put("CEDAR_IMPEX_HTTP_PORT", "0");
    environment.put("CEDAR_IMPEX_ADMIN_PORT", "0");
    environment.put("CEDAR_IMPEX_STOP_PORT", "0");
    CedarEnvironmentSource.setOverride(environment);
  }

  private static final DropwizardTestSupport<ImpexServerConfiguration> SERVER =
      new DropwizardTestSupport<>(ImpexServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  private static CedarConfig config;

  @BeforeAll
  public static void startServer() throws Exception {
    SERVER.before();
    config = CedarConfig.getInstance(CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_IMPEX));
    TestAuthUtil.installInMemoryUserService(config);
  }

  @AfterAll
  public static void stopServer() {
    SERVER.after();
  }

  static Stream<Arguments> requests() {
    return Stream.of(false,true).flatMap(chunked -> Stream.of(
        Arguments.of("abc","4",400,chunked), Arguments.of("abcde","4",400,chunked),
        Arguments.of("","4",400,chunked), Arguments.of("abcd","4",200,chunked),
        Arguments.of("abcd","not-a-number",400,chunked)));
  }
  @ParameterizedTest(name="payload={0}, size={1}, status={2}, chunked={3}") @MethodSource("requests")
  void validatesActualChunkBytesAndDoesNotCompleteOnRetry(String payload, String size, int expected, boolean chunked) throws Exception {
    String id = UUID.randomUUID().toString();
    String owner = TestAuthUtil.getTestUser1(config).getId();
    Path folder = Path.of(FlowUploadUtil.getUploadLocalFolderPath("impex-upload",owner,id));
    try {
      Map<String,String> fields = Map.of("uploadId",id,"numberOfFiles","1","flowChunkNumber","1",
          "flowChunkSize",size,"flowCurrentChunkSize","4","flowTotalSize","8","flowIdentifier","file",
          "flowFilename","data.xml","flowRelativePath","data.xml","flowTotalChunks","2");
      StringBuilder body = new StringBuilder();
      fields.forEach((name,value) -> body.append("--matrix\r\nContent-Disposition: form-data; name=\"")
          .append(name).append("\"\r\n\r\n").append(value).append("\r\n"));
      body.append("--matrix\r\nContent-Disposition: form-data; name=\"file\"; filename=\"data.xml\"\r\nContent-Type: application/octet-stream\r\n\r\n")
          .append(payload).append("\r\n--matrix--\r\n");
      var request = HttpRequest.newBuilder(URI.create("http://localhost:"+SERVER.getLocalPort()+"/command/import-cadsr-forms"))
          .header("Authorization",TestAuthUtil.getTestUser1AuthHeader(config))
          .header("Content-Type","multipart/form-data; boundary=matrix")
          .POST(chunked ? HttpRequest.BodyPublishers.ofInputStream(() -> new java.io.ByteArrayInputStream(
              body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)))
              : HttpRequest.BodyPublishers.ofString(body.toString())).build();
      var client = HttpClient.newHttpClient();
      for (int retry=0;retry<2;retry++) {
        var response = client.send(request,HttpResponse.BodyHandlers.ofString());
        assertEquals(expected,response.statusCode(),response.body());
      }
      var status = UploadManager.getInstance().getUploadStatus(owner,id);
      if (expected==200) {
        assertEquals(1,status.getFilesUploadStatus().get("file").getFileUploadedChunks());
        assertFalse(UploadManager.getInstance().isUploadComplete(owner,id));
        assertEquals(payload,Files.readString(folder.resolve("data.xml")));
      } else assertTrue(status == null || status.getFilesUploadStatus().isEmpty());
    } finally {
      UploadManager.getInstance().removeUploadStatus(owner,id);
      if (Files.exists(folder)) try (var paths = Files.walk(folder)) {
        for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
      }
    }
  }
}
