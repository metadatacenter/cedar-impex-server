package org.metadatacenter.submission.health;

import com.codahale.metrics.health.HealthCheck;

public class ImpexServerHealthCheck extends HealthCheck {

  public ImpexServerHealthCheck() {
  }

  @Override
  protected Result check() throws Exception {
    if (2 * 2 == 5) {
      return Result.unhealthy("Unhealthy, because 2 * 2 == 5");
    }
    return Result.healthy();
  }
}
