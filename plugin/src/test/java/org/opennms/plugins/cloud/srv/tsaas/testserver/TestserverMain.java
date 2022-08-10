package org.opennms.plugins.cloud.srv.tsaas.testserver;

import org.opennms.integration.api.v1.timeseries.InMemoryStorage;
import org.opennms.plugins.cloud.srv.tsaas.TsaasConfig;

/**
 * Starts a mock server to simulate the serverside of the grpc gateway.
 * It is backed by an in memory storage.
 * Only for local testing.
 */
public class TestserverMain {

  public static void main(String[] args) throws InterruptedException {
    TsaasConfig config = TsaasConfig.builder().build();
    TsaasServer server = new TsaasServer(config, new TsassServerInterceptor(), new InMemoryStorage());
    server.startServer();
    Thread.sleep(Long.MAX_VALUE); // wait till the end of time.
  }
}
