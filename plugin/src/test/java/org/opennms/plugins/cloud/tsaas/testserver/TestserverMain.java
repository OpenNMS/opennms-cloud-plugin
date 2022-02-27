package org.opennms.plugins.cloud.tsaas.testserver;

import org.opennms.integration.api.v1.timeseries.InMemoryStorage;

/**
 * Starts a mock server to simulate the serverside of the grpc gateway.
 * It is backed by an in memory storage.
 * Only for local testing.
 */
public class TestserverMain {

  private final static int PORT = 5001;

  public static void main(String[] args) throws InterruptedException {
    TimeseriesGrpcImpl timeseriesService = new TimeseriesGrpcImpl(new InMemoryStorage());
    TsaasServer server = new TsaasServer(timeseriesService, PORT, new TsassServerInterceptor());
    server.startServer();
    Thread.sleep(Long.MAX_VALUE); // wait till the end of time.
  }
}
