package org.opennms.plugins.cloud.tsaas;

import java.io.File;
import java.time.Duration;

import org.junit.After;
import org.junit.Before;

import org.junit.ClassRule;
import org.opennms.integration.api.v1.timeseries.AbstractStorageIntegrationTest;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class TsaasStorageTest extends AbstractStorageIntegrationTest {

    @ClassRule
    public static DockerComposeContainer<?> environment = new DockerComposeContainer<>(new File("src/test/resources/org/opennms/plugins/cloud/tsaas/docker-compose.yaml"))
        .withExposedService("cortex", 9009, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)))
        .withExposedService("tsaas", 5001, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10)));

    private TsaasStorage storage;

    @Before
    public void setUp() throws Exception {
        String host = "localhost";
        int port = 5001;
        String clientToken = "my-client";
        storage = new TsaasStorage(host, port, clientToken);
        super.setUp();
    }

    @After
    public void tearDown() {
        if (storage != null ) {
            storage.destroy();
        }
    }

    @Override
    protected TimeSeriesStorage createStorage() throws Exception {
        return storage;
    }
}
