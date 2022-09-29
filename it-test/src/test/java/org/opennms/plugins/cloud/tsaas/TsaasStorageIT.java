package org.opennms.plugins.cloud.tsaas;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

import org.apache.logging.log4j.util.Strings;
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
    String userHome = System.getProperty("user.home");
    Process p = Runtime.getRuntime().exec("/usr/bin/uname -a");

    BufferedReader stdInput = new BufferedReader(new
            InputStreamReader(p.getInputStream()));

    BufferedReader stdError = new BufferedReader(new
            InputStreamReader(p.getErrorStream()));

// Read the output from the command
    System.out.println("Here is the standard output of the command:\n");
    String s = null;
    while ((s = stdInput.readLine()) != null) {
      System.out.println(s);
    }

// Read any errors from the attempted command
    System.out.println("Here is the standard error of the command (if any):\n");
    while ((s = stdError.readLine()) != null) {
      System.out.println(s);
    }
    LOG.info("Starting docker-compose with \"USER_HOME\"={}", System.getProperty("user.home"));
    File karFile = new File(userHome, "/.m2/repository/org/opennms/plugins/cloud/kar/1.0.0-SNAPSHOT/kar-1.0.0-SNAPSHOT.kar");
    LOG.info("Checking for Kar file: {}: File {}", karFile, karFile.exists());
    environment = new DockerComposeContainer<>(new File("src/test/resources/docker-compose.yaml"))
        .withEnv("USER_HOME", userHome)
        .withTailChildContainers(true)
        .withExposedService("database_1", 5432, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(15)))
        .withLogConsumer("database_1", new Slf4jLogConsumer(LOG))
        .withExposedService("horizon_1", 8980, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)))
        .withLogConsumer("horizon_1", new Slf4jLogConsumer(LOG));
    try {
      environment.start();
    } catch(Exception ex){
      ex.printStackTrace();
      var container = environment.getContainerByServiceName("horizon_1");
      if(container.isPresent()){
        LOG.info("isCreated:{}, isRunning: {}, isHealthy: {}", container.get().isCreated(), container.get().isRunning(), container.get().isHealthy());
        LOG.info(container.get().getLogs());
      }
    }
  }

  @Before
  public void setUp() {
    this.karafShell = new KarafShell(InetSocketAddress.createUnresolved("localhost", 8101));
  }

  @Test
  public void canInstallAndStartPlugin() {

    // install plugin for core
    karafShell.runCommand("kar:install --no-start mvn:org.opennms.plugins.cloud/kar/1.0.0-SNAPSHOT/kar");
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
