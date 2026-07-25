package org.metadatacenter.impex;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Boots the real application through Dropwizard test support and exercises the wiring no
 * backend is needed for. This catches configuration and startup rot that a config-only test
 * cannot see.
 */
public class ImpexServerApplicationSmokeTest {

  public static final DropwizardTestSupport<ImpexServerConfiguration> SERVER =
      new DropwizardTestSupport<>(ImpexServerApplication.class, ResourceHelpers.resourceFilePath("test-config.yml"));

  private static final HttpClient CLIENT = HttpClient.newHttpClient();

  @BeforeAll
  public static void oneTimeSetUp() {
    SERVER.before();
  }

  @AfterAll
  public static void oneTimeTearDown() {
    SERVER.after();
  }

  private HttpResponse<String> get(String path) throws Exception {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + path))
        .GET()
        .build();
    return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
  }

  @Test
  public void indexIsServed() throws Exception {
    HttpResponse<String> response = get("/");
    Assertions.assertEquals(200, response.statusCode());
    Assertions.assertTrue(response.body().contains("name"));
  }

}
