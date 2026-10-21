package org.metadatacenter.cadsr.ingestor.util;

/**
 * The caDSR tools reach CEDAR through CedarServerUtil, whose address override is package-private. This class shares
 * that package, so the impex server's tests can point the tools at a local stand-in for CEDAR and back.
 */
public final class CadsrToolsServers {

  private CadsrToolsServers() {
  }

  public static void pointAt(String baseUrl) {
    CedarServerUtil.overrideServerBaseUrl(baseUrl);
  }

  public static void reset() {
    CedarServerUtil.overrideServerBaseUrl(null);
  }
}
