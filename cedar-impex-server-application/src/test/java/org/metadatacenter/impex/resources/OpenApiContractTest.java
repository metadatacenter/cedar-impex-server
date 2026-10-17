package org.metadatacenter.impex.resources;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.metadatacenter.util.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The caDSR upload documents its refusal of a chunk as the shared error body, which names the problem. */
class OpenApiContractTest {

  @Test
  void theUploadDocumentsARefusedChunkAsTheSharedErrorBody() throws IOException {
    JsonNode refusal = readSpec().at("/paths/~1command~1import-cadsr-forms/post/responses/400");
    assertEquals("#/components/schemas/CedarError",
        refusal.at("/content/application~1json/schema/$ref").asText(), refusal.toString());
    assertTrue(refusal.path("description").asText().contains("message names the problem"), refusal.toString());
  }

  private static JsonNode readSpec() throws IOException {
    try (InputStream input = OpenApiContractTest.class.getResourceAsStream("/assets/swagger-api/swagger.json")) {
      assertNotNull(input, "generated OpenAPI document");
      return JsonMapper.STRICT_MAPPER.readTree(input);
    }
  }
}
