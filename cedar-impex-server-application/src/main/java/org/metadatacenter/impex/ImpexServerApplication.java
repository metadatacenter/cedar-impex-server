package org.metadatacenter.impex;

import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.metadatacenter.cedar.util.dw.CedarMicroserviceApplication;
import org.metadatacenter.config.CedarConfig;
import org.metadatacenter.impex.health.ImpexServerHealthCheck;
import org.metadatacenter.impex.resources.IndexResource;
import org.metadatacenter.impex.resources.NcbiCairrSubmissionServerResource;
import org.metadatacenter.impex.resources.NcbiGenericSubmissionServerResource;
import org.metadatacenter.model.ServerName;
import org.metadatacenter.impex.ncbi.queue.NcbiSubmissionExecutorService;
import org.metadatacenter.impex.ncbi.queue.NcbiSubmissionQueueProcessor;
import org.metadatacenter.impex.ncbi.queue.NcbiSubmissionQueueService;
import org.metadatacenter.impex.notifications.StatusNotifier;

public class ImpexServerApplication extends CedarMicroserviceApplication<ImpexServerConfiguration> {

  private static NcbiSubmissionExecutorService ncbiSubmissionExecutorService;
  private static NcbiSubmissionQueueService ncbiSubmissionQueueService;

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
    ncbiSubmissionQueueService = new NcbiSubmissionQueueService(cedarConfig.getCacheConfig().getPersistent());

    NcbiSubmissionQueueService ncbiSubmissionQueueService = new NcbiSubmissionQueueService(cedarConfig.getCacheConfig().getPersistent());

    NcbiGenericSubmissionServerResource.injectServices(ncbiSubmissionQueueService);
    NcbiCairrSubmissionServerResource.injectServices(ncbiSubmissionQueueService);

    ncbiSubmissionExecutorService = new NcbiSubmissionExecutorService(cedarConfig);

    StatusNotifier.initialize(cedarConfig);
  }

  @Override
  public void runApp(ImpexServerConfiguration configuration, Environment environment) {

    final IndexResource index = new IndexResource();
    environment.jersey().register(index);

    // Register resources

    final NcbiGenericSubmissionServerResource ncbiSubmissionServerResource = new NcbiGenericSubmissionServerResource(cedarConfig);
    environment.jersey().register(ncbiSubmissionServerResource);

    final NcbiCairrSubmissionServerResource cairrSubmissionServerResource = new NcbiCairrSubmissionServerResource(cedarConfig);
    environment.jersey().register(cairrSubmissionServerResource);

    final ImpexServerHealthCheck healthCheck = new ImpexServerHealthCheck();
    environment.healthChecks().register("message", healthCheck);

    // NCBI submission processor
    NcbiSubmissionQueueProcessor ncbiSubmissionProcessor = new NcbiSubmissionQueueProcessor(ncbiSubmissionQueueService,
        ncbiSubmissionExecutorService);
    environment.lifecycle().manage(ncbiSubmissionProcessor);
  }
}
