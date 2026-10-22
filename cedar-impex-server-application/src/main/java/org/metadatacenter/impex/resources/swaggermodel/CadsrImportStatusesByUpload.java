package org.metadatacenter.impex.resources.swaggermodel;

import io.swagger.v3.oas.annotations.media.Schema;
import org.metadatacenter.impex.imp.cadsr.CadsrImportStatus;

/**
 * Documentation-only model of the map the import-status route returns when no upload is named.
 *
 * <p>A {@code @Schema} annotation cannot carry a type argument, so the {@code Map<String,
 * CadsrImportStatus>} the handler serializes has no schema of its own. This class names the map's
 * value type instead. It does not extend the map: introspecting {@code HashMap} documents its
 * {@code isEmpty()} as a property no client ever sees. It has no runtime role.</p>
 */
@Schema(name = "CadsrImportStatusesByUpload", type = "object",
    description = "Every import the caller has run, keyed by upload identifier.",
    additionalPropertiesSchema = CadsrImportStatus.class)
public final class CadsrImportStatusesByUpload {

  private CadsrImportStatusesByUpload() {
  }
}
