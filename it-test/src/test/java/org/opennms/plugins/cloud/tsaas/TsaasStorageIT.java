package org.opennms.plugins.cloud.tsaas;

import java.io.File;
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
  public static void beforeAll() {
    LOG.info("Starting docker-compose with \"USER_HOME\"={}", System.getProperty("user.home"));
    environment = new DockerComposeContainer<>(new File("src/test/resources/docker-compose.yaml"))
        .withEnv("USER_HOME", System.getProperty("user.home"))
        .withTailChildContainers(true)
        .withExposedService("database", 5432, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(15)))
        .withLogConsumer("database", new Slf4jLogConsumer(LOG))
        .withExposedService("horizon", 8980, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)))
        .withLogConsumer("horizon", new Slf4jLogConsumer(LOG));
    environment.start();
  }

  @Before
  public void setUp() {
    this.karafShell = new KarafShell(InetSocketAddress.createUnresolved("localhost", 8101));
  }

  @Test
  public void canInstallAndStartPlugin() {

    // install plugin for core
    karafShell.runCommand("kar:install --no-start file:/usr/share/opennms/deploy/kar-1.0.0-SNAPSHOT.kar");
    karafShell.runCommand("feature:repo-add mvn:org.opennms.plugins.cloud/karaf-features/1.0.0-SNAPSHOT/xml");
    karafShell.runCommand("feature:install opennms-cloud-plugin");

    // check if plugin has been started, if so we assume the installation worked well.
    karafShell.runCommand("feature:list | grep opennms-cloud-plugin-core", output -> output.contains("Started"));
  }

  @AfterClass
  public static void afterAll() {
    if (environment != null) {
      environment.stop();
    }
  }
}
