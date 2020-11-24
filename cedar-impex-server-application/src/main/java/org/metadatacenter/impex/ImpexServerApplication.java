package org.metadatacenter.impex;

import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.impex.health.ImpexServerHealthCheck;
import org.metadatacenter.impex.resources.IndexResource;
import org.metadatacenter.impex.resources.ImpexServerResource;
import org.metadatacenter.model.ServerName;

public class ImpexServerApplication extends CedarMicroserviceApplication<ImpexServerConfiguration> {

  public static void main(String[] args) throws Exception {
    new ImpexServerApplication().run(args);
  }

  @Override
  protected ServerName getServerName() {
    return ServerName.IMPEX;
  }

  @Override
  protected void initializeWithBootstrap(Bootstrap<ImpexServerConfiguration> bootstrap, CedarConfig cedarConfig) {
  }

  @Override
  public void initializeApp() {

  }

  @Override
  public void runApp(ImpexServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    // Register resources
    final ImpexServerResource ncbiSubmissionServerResource = new ImpexServerResource(cedarConfig);
    environment.jersey().register(ncbiSubmissionServerResource);

    final ImpexServerHealthCheck healthCheck = new ImpexServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);

  }
}
