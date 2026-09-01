package org.metadatacenter.impex.config;

import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.model.SystemComponent;
import org.metadatacenter.util.test.AbstractCedarConfigTest;

public class CedarConfigImpexTest extends AbstractCedarConfigTest {

  @Override
  protected SystemComponent getSystemComponent() {
    return SystemComponent.SERVER_IMPEX;
  }

  /**
   * The caDSR administrator's API key is the impex server's alone. Every other component loads the
   * same {@code caDSRAdminUser} section with its placeholder intact.
   */
  @Override
  protected void assertServerSpecificConfig(CedarConfig config) {
    assertResolved("caDSRAdminUser.apiKey", config.getCaDSRAdminUserConfig().getApiKey());
  }

}
