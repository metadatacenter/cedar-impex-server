package org.metadatacenter.impex;

import io.dropwizard.testing.DropwizardTestSupport;
import io.dropwizard.testing.ResourceHelpers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.metadatacenter.config.environment.CedarEnvironmentSource;
import org.metadatacenter.impex.resources.ImpexServerResource;
import org.metadatacenter.util.json.JsonMapper;
import org.metadatacenter.util.test.RouteSurface;
import org.metadatacenter.util.test.TestHttpClient;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Route safety net: probes every endpoint the impex resource declares, unauthenticated, and
 * requires each to answer 401. A 404/405 means the route vanished or changed verb; any other status
 * means an endpoint lost its authentication assertion. No fixtures and no backend are involved.
 */
public class ImpexRoutesRespondTest {

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

  @BeforeAll
  public static void startServer() throws Exception {
    SERVER.before();
  }

  @AfterAll
  public static void stopServer() {
    SERVER.after();
  }

  @Test
  public void everyRouteRejectsAnUnauthenticatedRequest() {
    RouteSurface.assertEveryRouteAnswers(
        "http://localhost:" + SERVER.getLocalPort(),
        RouteSurface.endpoints(ImpexServerResource.class),
        401);
  }

  /**
   * A request that will not accept a JSON answer is refused with 406 in the shared error body, which
   * the OpenAPI document lists on every route. Content negotiation happens during dispatch, before the
   * endpoint's authentication assertion, so this is observable without credentials.
   */
  @Test
  public void aRequestRefusingJsonIsRefusedInTheSharedErrorBody() throws Exception {
    StringBuilder failures = new StringBuilder();
    for (RouteSurface.Endpoint endpoint : RouteSurface.endpoints(ImpexServerResource.class)) {
      HttpRequest.Builder request = HttpRequest.newBuilder()
          .uri(URI.create("http://localhost:" + SERVER.getLocalPort() + RouteSurface.resolvedPath(endpoint)))
          .header("Accept", "text/html");
      if (endpoint.verb.equals("GET")) {
        request.GET();
      } else {
        request.header("Content-Type", RouteSurface.contentTypeFor(endpoint))
            .method(endpoint.verb, HttpRequest.BodyPublishers.ofString("{}"));
      }
      HttpResponse<String> response = TestHttpClient.send(request.build());
      int bodyStatus = response.statusCode() == 406
          ? JsonMapper.STRICT_MAPPER.readTree(response.body()).path("statusCode").asInt() : 0;
      if (bodyStatus != 406) {
        failures.append(endpoint.key()).append(": ").append(response.statusCode()).append(' ')
            .append(response.body()).append('\n');
      }
    }
    Assertions.assertEquals(0, failures.length(), "Not refused with 406 in the shared error body:\n" + failures);
  }

}
