package org.metadatacenter.impex;

import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceIndexResource;
import org.metadatacenter.cedar.util.dw.CedarDefaultHealthCheck;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.impex.imp.cadsr.CadsrImportStatusManager;
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

    final CedarMicroserviceIndexResource index =
        new CedarMicroserviceIndexResource(cedarConfig, getServerName());
    environment.jersey().register(index);

    // Register resources
    final ImpexServerResource ncbiSubmissionServerResource = new ImpexServerResource(cedarConfig);
    environment.jersey().register(ncbiSubmissionServerResource);

    final CedarDefaultHealthCheck healthCheck = new CedarDefaultHealthCheck();
    environment.healthChecks().register("message", healthCheck);

    // Stop the caDSR import-status cleaner's scheduled executor cleanly on server shutdown.
    environment.lifecycle().manage(new Managed() {
      @Override
      public void start() {
      }

      @Override
      public void stop() {
        CadsrImportStatusManager.shutdownIfRunning();
      }
    });

  }
}
