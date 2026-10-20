package org.metadatacenter.impex;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.metadatacenter.cadsr.ingestor.util.CadsrToolsServers;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.config.environment.CedarEnvironmentVariableProvider;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.TestAuthUtil;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A caDSR form uploaded to the impex server becomes a template in the folder the caller names. The upload arrives
 * in chunks, and once its last chunk is stored the server translates each form in the background and creates the
 * template on the caller's behalf. The caller follows the import through the status endpoint, which shows each file
 * as pending, in progress, complete or in error, and shows a caller only their own imports.
 * <p>
 * A local server answers for CEDAR: it holds none of the form's CDEs, and creates every template it is sent.
 */
@Tag("integration")
public class ImpexFormImportHttpTest {

  static {
    Map<String, String> environment = new HashMap<>(CedarEnvironmentSource.getAll());
    environment.put("CEDAR_IMPEX_HTTP_PORT", "0");
    environment.put("CEDAR_IMPEX_ADMIN_PORT", "0");
    environment.put("CEDAR_IMPEX_STOP_PORT", "0");
    CedarEnvironmentSource.setOverride(environment);
  }

  private static final DropwizardTestSupport<ImpexServerConfiguration> SERVER =
      new DropwizardTestSupport<>(ImpexServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));
  private static final String FOLDER_UUID = "0f2a3b4c-1111-2222-3333-444455556666";

  private static CedarConfig config;
  private static HttpServer cedar;
  private static final List<String[]> templatesCreated = new CopyOnWriteArrayList<>();

  @BeforeAll
  public static void startServers() throws Exception {
    SERVER.before();
    config = CedarConfig.getInstance(CedarEnvironmentVariableProvider.getFor(SystemComponent.SERVER_IMPEX));
    TestAuthUtil.installInMemoryUserService(config);
    cedar = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    cedar.createContext("/", exchange -> {
      String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      String reply;
      int status;
      if (exchange.getRequestURI().getPath().equals("/resource/search")) {
        status = 200;
        reply = "{\"totalCount\": 0, \"resources\": []}";
      } else if (exchange.getRequestURI().getPath().equals("/resource/templates")) {
        templatesCreated.add(new String[]{exchange.getRequestURI().getRawQuery(),
            exchange.getRequestHeaders().getFirst("Authorization"), body});
        status = 201;
        reply = "{\"@id\": \"https://repo.metadatacenter.orgx/templates/" + UUID.randomUUID() + "\"}";
      } else {
        status = 404;
        reply = "{}";
      }
      byte[] bytes = reply.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(status, bytes.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(bytes);
      }
    });
    cedar.start();
    CadsrToolsServers.pointAt("http://127.0.0.1:" + cedar.getAddress().getPort());
  }

  @AfterAll
  public static void stopServers() {
    CadsrToolsServers.reset();
    cedar.stop(0);
    SERVER.after();
  }

  @Test
  void anUploadedFormBecomesATemplateInTheNamedFolder() throws Exception {
    byte[] form;
    try (InputStream in = getClass().getResourceAsStream("/xml/examples/forms/form2.xml")) {
      form = in.readAllBytes();
    }
    String uploadId = UUID.randomUUID().toString();

    assertEquals(200, upload(uploadId, "form2.xml", form, "?folderId=" + FOLDER_UUID).statusCode());
    JsonNode file = finished(uploadId, "form2.xml");

    assertEquals("COMPLETE", file.get("importStatus").asText(), file.toString());
    assertTrue(file.get("report").asText().contains("Form translation complete"), file.toString());
    String[] created = templatesCreated.stream().filter(t -> t[2].contains("2725838")).findFirst()
        .orElseThrow(() -> new AssertionError("no template was created: " + templatesCreated.size()));
    assertEquals("folder_id=folders%2F" + FOLDER_UUID, created[0]);
    assertEquals(TestAuthUtil.getTestUser1AuthHeader(config), created[1], "the template is created as the caller");
    assertEquals("2725838", JsonMapper.STRICT_MAPPER.readTree(created[2]).get("schema:identifier").asText());
  }

  @Test
  void aFileThatIsNotACadsrFormEndsInError() throws Exception {
    String uploadId = UUID.randomUUID().toString();

    assertEquals(200, upload(uploadId, "notes.xml", "not a form".getBytes(StandardCharsets.UTF_8), "?folderId="
        + FOLDER_UUID).statusCode());
    JsonNode file = finished(uploadId, "notes.xml");

    assertEquals("ERROR", file.get("importStatus").asText(), file.toString());
    assertTrue(file.get("report").asText().contains("Parsing error"), file.toString());
  }

  /** Another user can neither read an import by its identifier nor see it among their own. */
  @Test
  void aCallerSeesOnlyTheirOwnImports() throws Exception {
    String uploadId = UUID.randomUUID().toString();
    upload(uploadId, "notes.xml", "not a form".getBytes(StandardCharsets.UTF_8), "?folderId=" + FOLDER_UUID);
    finished(uploadId, "notes.xml");

    assertTrue(status(TestAuthUtil.getTestUser1AuthHeader(config), null).body().contains(uploadId));
    String user2 = TestAuthUtil.getTestUser2AuthHeader(config);
    assertEquals(404, status(user2, uploadId).statusCode());
    assertFalse(status(user2, null).body().contains(uploadId));
  }

  private static HttpResponse<String> upload(String uploadId, String fileName, byte[] content, String query)
      throws Exception {
    Map<String, String> fields = Map.of("uploadId", uploadId, "numberOfFiles", "1", "flowChunkNumber", "1",
        "flowChunkSize", String.valueOf(content.length), "flowCurrentChunkSize", String.valueOf(content.length),
        "flowTotalSize", String.valueOf(content.length), "flowIdentifier", "file-" + uploadId,
        "flowFilename", fileName, "flowRelativePath", fileName, "flowTotalChunks", "1");
    StringBuilder head = new StringBuilder();
    fields.forEach((name, value) -> head.append("--matrix\r\nContent-Disposition: form-data; name=\"").append(name)
        .append("\"\r\n\r\n").append(value).append("\r\n"));
    head.append("--matrix\r\nContent-Disposition: form-data; name=\"file\"; filename=\"").append(fileName)
        .append("\"\r\nContent-Type: application/octet-stream\r\n\r\n");
    byte[] start = head.toString().getBytes(StandardCharsets.UTF_8);
    byte[] end = "\r\n--matrix--\r\n".getBytes(StandardCharsets.UTF_8);
    byte[] body = new byte[start.length + content.length + end.length];
    System.arraycopy(start, 0, body, 0, start.length);
    System.arraycopy(content, 0, body, start.length, content.length);
    System.arraycopy(end, 0, body, start.length + content.length, end.length);
    HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/command/import-cadsr-forms" + query))
        .header("Authorization", TestAuthUtil.getTestUser1AuthHeader(config))
        .header("Content-Type", "multipart/form-data; boundary=matrix")
        .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  /** Polls the upload's status until the file's import has ended, and answers the file's status. */
  private static JsonNode finished(String uploadId, String fileName) throws Exception {
    long deadline = System.currentTimeMillis() + 30_000;
    JsonNode file = null;
    while (System.currentTimeMillis() < deadline) {
      HttpResponse<String> response = status(TestAuthUtil.getTestUser1AuthHeader(config), uploadId);
      if (response.statusCode() == 200) {
        file = JsonMapper.STRICT_MAPPER.readTree(response.body()).path("filesImportStatus").path(fileName);
        String state = file.path("importStatus").asText();
        if (state.equals("COMPLETE") || state.equals("ERROR")) {
          return file;
        }
      }
      Thread.sleep(100);
    }
    throw new AssertionError("the import of " + fileName + " did not end: " + file);
  }

  private static HttpResponse<String> status(String authorization, String uploadId) throws Exception {
    String query = uploadId == null ? "" : "?uploadId=" + uploadId;
    HttpRequest request = HttpRequest.newBuilder(URI.create(base() + "/command/import-cadsr-forms-status" + query))
        .header("Authorization", authorization).GET().build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static String base() {
    return "http://localhost:" + SERVER.getLocalPort();
  }
}
