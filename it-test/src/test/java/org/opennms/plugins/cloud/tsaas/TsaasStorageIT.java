package org.opennms.plugins.cloud.tsaas;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.opennms.plugins.cloud.ittest.TestserverMain.TEST_PORT;
import static org.opennms.plugins.cloud.testserver.FileUtil.classpathFileToString;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * This test checks if the plugin can be successfully installed in OpenNMS:
 * It starts a opennms container and installs the plugin into it.
 * => The Osgi wiring / dependencies are checked.
 * It does NOT test the functionality of the plugin itself.
 */
public class TsaasStorageIT {

    private static final Logger LOG = LoggerFactory.getLogger(TsaasStorageIT.class);

    public static DockerComposeContainer<?> environment;

    private KarafShell karafShell;

    @BeforeClass
    public static void beforeAll() throws IOException, InterruptedException {
        environment = new DockerComposeContainer<>(new File("src/test/resources/docker-compose.yaml"))
                .withEnv("USER_HOME", System.getProperty("user.home"))
                .withTailChildContainers(true)
                .withExposedService("database_1", 5432, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(15)))
                .withLogConsumer("database_1", new Slf4jLogConsumer(LOG))
                .withExposedService("horizon_1", 8980, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)))
                .withLogConsumer("horizon_1", new Slf4jLogConsumer(LOG));
        environment.start();
        LOG.info("Started horizon_1 container with name= {}, to connect to bash use: 'docker exec -it {}  bash'",
                environment.getContainerByServiceName("horizon_1").get().getContainerInfo().getName(),
                environment.getContainerByServiceName("horizon_1").get().getContainerInfo().getName());
        // run MockCloud server in OpenNMS container:
        String stdout =  environment
                .getContainerByServiceName("horizon_1")
                .orElseThrow(() -> new IllegalArgumentException("container horizon_1 not found"))
                .execInContainer("sh", "-c", "java -cp /usr/share/opennms/.m2/repository/org/opennms/plugins/cloud/it-test/1.0.0-SNAPSHOT/it-test-1.0.0-SNAPSHOT-jar-with-dependencies.jar org.opennms.plugins.cloud.ittest.TestserverMain &")
                .getStdout();
        assertNotNull(stdout);
        LOG.info(stdout);
        assertTrue("Could not find 'MockCloud Server started' in output.", stdout.contains("MockCloud Server started"));
    }

    @Before
    public void setUp() {
        this.karafShell = new KarafShell(InetSocketAddress.createUnresolved("localhost", 8101));
    }

    @Test
    public void canInstallAndStartPlugin() {

        // install plugin for core
        karafShell.runCommand("kar:install --no-start mvn:org.opennms.plugins.cloud/kar/1.0.0-SNAPSHOT/kar");
        karafShell.runCommand("feature:install opennms-cloud-plugin-core");

        // check if plugin has been started, if so we assume the installation worked well.
        karafShell.runCommand("feature:list | grep opennms-cloud-plugin-core", output -> output.contains("Started"));

        // configure endpoints
        String config = String.format(
                "config:edit org.opennms.plugins.cloud%n" +
                        "property-set pas.tls.host localhost%n" +
                        "property-set pas.tls.port %s%n" +
                        "property-set pas.tls.security TLS%n" +
                        "property-set pas.mtls.host localhost%n" +
                        "property-set pas.mtls.port %s%n" +
                        "property-set pas.mtls.security MTLS%n" + // must always be MTLS
                        "property-set grpc.truststore \"%s\"%n" +
                        "config:update",
                TEST_PORT,
                TEST_PORT,
                classpathFileToString("/cert/clienttruststore.pem")
        );
        karafShell.runCommand(config);

        // init plugin: this gets the MTLS certs, the enabled services and tokens
        // all enabled services are configured and started.
        karafShell.runCommand("opennms-cloud:init myApiToken", output -> output.contains("was successful"));
    }



    @AfterClass
    public static void afterAll() {
        if (environment != null) {
            environment.stop();
        }
    }
}
