package org.opennms.plugins.cloud.tsaas.testserver;

import org.opennms.plugins.cloud.tsaas.TsaasConfig;

/**
 * Starts a mock server to simulate the serverside of the grpc gateway.
 * It is backed by an in memory storage.
 * Only for local testing.
 */
public class TestserverMain {

  private final static int PORT = 5001;

  public static void main(String[] args) throws InterruptedException {
    TsaasConfig config = TsaasConfig.builder().build();
    TsaasServer server = new TsaasServer(config, new TsassServerInterceptor());
    server.startServer();
    Thread.sleep(Long.MAX_VALUE); // wait till the end of time.
  }
}
