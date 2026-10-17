package org.metadatacenter.impex.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The caDSR routes document every answer they give, each refusal in the shared error body. The upload
 * documents its refusal of a chunk as the shared error body, which names the problem.
 */
class OpenApiContractTest {

  @Test
  void theUploadDocumentsARefusedChunkAsTheSharedErrorBody() throws IOException {
    JsonNode refusal = readSpec().at("/paths/~1command~1import-cadsr-forms/post/responses/400");
    assertEquals("#/components/schemas/CedarError",
        refusal.at("/content/application~1json/schema/$ref").asText(), refusal.toString());
    assertTrue(refusal.path("description").asText().contains("message names the problem"), refusal.toString());
  }

  @Test
  void theUploadDocumentsEveryAnswer() throws IOException {
    assertAnswers(readSpec().at("/paths/~1command~1import-cadsr-forms/post"), "400", "401", "406", "415", "429",
        "500", "503");
  }

  @Test
  void theImportStatusDocumentsEveryAnswer() throws IOException {
    assertAnswers(readSpec().at("/paths/~1command~1import-cadsr-forms-status/get"), "401", "404", "406", "429",
        "500", "503");
  }

  private static void assertAnswers(JsonNode operation, String... refusals) {
    JsonNode responses = operation.path("responses");
    Set<String> documented = new TreeSet<>();
    responses.fieldNames().forEachRemaining(documented::add);
    Set<String> expected = new TreeSet<>(Set.of(refusals));
    expected.add("200");
    assertEquals(expected, documented);
    for (String status : refusals) {
      assertEquals("#/components/schemas/CedarError",
          responses.at("/" + status + "/content/application~1json/schema/$ref").asText(), status);
      assertFalse(responses.path(status).path("description").asText().isBlank(), status);
    }
  }

  private static JsonNode readSpec() throws IOException {
    try (InputStream input = OpenApiContractTest.class.getResourceAsStream("/assets/swagger-api/swagger.json")) {
      assertNotNull(input, "generated OpenAPI document");
      return JsonMapper.STRICT_MAPPER.readTree(input);
    }
  }
}
