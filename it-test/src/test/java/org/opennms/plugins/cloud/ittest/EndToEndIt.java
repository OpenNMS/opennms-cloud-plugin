/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.plugins.cloud.ittest;

import static java.lang.String.format;
import static org.opennms.plugins.cloud.ittest.MockCloudMain.MOCK_CLOUD_HOST;
import static org.opennms.plugins.cloud.ittest.MockCloudMain.MOCK_CLOUD_PORT;
import static org.opennms.plugins.cloud.testserver.FileUtil.classpathFileToString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * This test checks if the plugin can be successfully installed in OpenNMS:
 * It starts a opennms container and installs the plugin into it.
 * => The Osgi wiring / dependencies are checked.
 * It does NOT test the functionality of the plugin itself.
 */
public class EndToEndIt {

    private static final Logger LOG = LoggerFactory.getLogger(EndToEndIt.class);
    private static final Pattern OFFHEAPLOG_PATTERN = Pattern.compile(".*OffheapTimeSeriesWriter: .*[Bb]ufferSize: "
            + "(?<BUFFERSIZE>\\d+), numWriterThreads: (?<WRITERTHREAD>\\d+), batchSize: (?<BATCHSIZE>\\d+), "
            + "path: (?<PATH>[^,]+), maxFileSize: (?<MAXFILESIZE>\\d+)");

    public static DockerComposeContainer<?> environment;

    private KarafShell opennmsShell;
    private KarafShell sentinelShell;

    private static String pluginVersion;

    @BeforeClass
    public static void beforeAll() throws IOException, XmlPullParserException {
        MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
        Model model = mavenXpp3Reader.read(new FileReader("pom.xml"));
        pluginVersion = model.getParent().getVersion();
        environment = new DockerComposeContainer<>(new File("src/test/resources/docker-compose.yaml"))
                .withEnv("USER_HOME", System.getProperty("user.home"))
                .withEnv("PLUGIN_VERSION", pluginVersion)
                .withTailChildContainers(true)
                .withExposedService("database_1", 5432, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(15)))
                .withLogConsumer("database_1", new Slf4jLogConsumer(LOG))
                .withExposedService("horizon_1", 8980, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)))
                .withExposedService("horizon_1", 8101) // Karaf Shell
                .withLogConsumer("horizon_1", new Slf4jLogConsumer(LOG))
                .withExposedService("sentinel_1", 8301, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30))) // Karaf Shell
                .withLogConsumer("sentinel_1", new Slf4jLogConsumer(LOG))
                .withExposedService("cloudMock_1", MOCK_CLOUD_PORT, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30)))
                .withLogConsumer("cloudMock_1", new Slf4jLogConsumer(LOG));
        environment.start();

        // fix me: this should be done in docker-compose but I couldn't get it to work:
        getContainer("sentinel_1")
                .copyFileToContainer(MountableFile.forClasspathResource(
                                "/overlay/sentinel/usr/share/sentinel/etc/org.opennms.netmgt.distributed.datasource.cfg"),
                        "/usr/share/sentinel/etc/org.opennms.netmgt.distributed.datasource.cfg");

        printContainerStartup("horizon_1");
        printContainerStartup("sentinel_1");
    }

    @Before
    public void setUp() {
        int opennmsShellPort = environment.getServicePort("horizon_1", 8101);
        this.opennmsShell = new KarafShell(InetSocketAddress.createUnresolved("localhost", opennmsShellPort));
        int sentinelShellPort = environment.getServicePort("sentinel_1", 8301);
        this.sentinelShell = new KarafShell(InetSocketAddress.createUnresolved("localhost", sentinelShellPort));
    }

    @Test
    @SuppressWarnings("java:S2699") // no assertions because we are an integration test and test against karaf shell
    public void canInstallAndStartPlugin() throws IOException, InterruptedException {
        checkProperties();
        installAndStartPluginInOpenNms();
        installAndStartPluginInSentinel();
    }

    private void installAndStartPluginInOpenNms() {

        // install plugin for core
        opennmsShell.runCommand(format("kar:install --no-start mvn:org.opennms.plugins.cloud.assembly/org.opennms.plugins.cloud.assembly.kar/%s/kar", pluginVersion));
        opennmsShell.runCommand("feature:install opennms-plugin-cloud-core");

        // check if plugin has been started, if so we assume the installation worked well.
        opennmsShell.runCommand("feature:list | grep opennms-plugin-cloud-core", output -> output.contains("Started"));

        // configure plugin to use MockCloud on localhost
        String config = createConfig();
        opennmsShell.runCommand(config);

        // init plugin: this gets the MTLS certs, the enabled services and tokens from PAS
        // all enabled services are configured and started.
        opennmsShell.runCommand("opennms-cloud:init myApiToken", output -> output.contains("Initialization of cloud plugin in OPENNMS was successful."));
    }

    private void installAndStartPluginInSentinel() {

        // install plugin for core
        sentinelShell.runCommand(format("kar:install --no-start mvn:org.opennms.plugins.cloud.assembly/org.opennms.plugins.cloud.assembly.kar/%s/kar", pluginVersion));
        sentinelShell.runCommand("feature:install opennms-plugin-cloud-sentinel");

        // check if plugin has been started, if so we assume the installation worked well.
        sentinelShell.runCommand("feature:list | grep opennms-plugin-cloud-sentinel", output -> output.contains("Started"));

        // configure plugin to use MockCloud on core
        String config = createConfig();
        sentinelShell.runCommand(config);

        // init plugin: this gets the MTLS certs, the enabled services and tokens from the KV store out of the database,
        // => thus no token is needed.
        // all enabled services are configured and started.
        sentinelShell.runCommand("opennms-cloud:init", output -> output.contains("Initialization of cloud plugin in SENTINEL was successful."));
    }

    private String createConfig() {
        return format(
                "config:edit org.opennms.plugins.cloud%n"
                        + "property-set pas.tls.host %s%n"
                        + "property-set pas.tls.port %s%n"
                        + "property-set pas.tls.security TLS%n"
                        + "property-set pas.mtls.host %s%n"
                        + "property-set pas.mtls.port %s%n"
                        + "property-set pas.mtls.security MTLS%n"
                        + "property-set grpc.truststore \"%s\"%n"
                        + "config:update",
                MOCK_CLOUD_HOST,
                MOCK_CLOUD_PORT,
                MOCK_CLOUD_HOST,
                MOCK_CLOUD_PORT,
                classpathFileToString("/cert/horizon/clienttruststore.pem")
        );
    }

    private static ContainerState getContainer(final String container) {
        return environment.getContainerByServiceName(container)
                .orElseThrow(() -> new IllegalArgumentException(format("container %s not found", container)));
    }

    private static void printContainerStartup(String container) {
        String containerName = getContainer(container)
                .getContainerInfo()
                .getName();
        LOG.info("Started {} container with name= {}, to connect to bash use: 'docker exec -it {}  bash'",
                container,
                containerName,
                containerName);
    }

    private void checkProperties() throws IOException, InterruptedException {
        Properties p = readTimeseriesProperties();
        Container.ExecResult result = environment.getContainerByServiceName("horizon_1").get()
                .execInContainer("grep", "OffheapTimeSeriesWriter", "logs/eventd.log");
        Assert.assertEquals(0, result.getExitCode());
        Matcher matcher = OFFHEAPLOG_PATTERN.matcher(result.getStdout());
        Assert.assertTrue(matcher.find());
        Assert.assertEquals(p.getProperty("org.opennms.timeseries.config.writer_threads"), matcher.group("WRITERTHREAD"));
        Assert.assertEquals(p.getProperty("org.opennms.timeseries.config.buffer_size"), matcher.group("BUFFERSIZE"));
        Assert.assertEquals(p.getProperty("org.opennms.timeseries.config.offheap.batch_size"), matcher.group("BATCHSIZE"));
        Assert.assertEquals(p.getProperty("org.opennms.timeseries.config.offheap.path"), matcher.group("PATH"));
        String strMaxFileSize = p.getProperty("org.opennms.timeseries.config.offheap.max_file_size");
        strMaxFileSize = "-1".equals(strMaxFileSize) ? String.valueOf(Long.MAX_VALUE) : strMaxFileSize;
        Assert.assertEquals(strMaxFileSize, matcher.group("MAXFILESIZE"));
    }

    private Properties readTimeseriesProperties() throws IOException {
        try (InputStream input = new FileInputStream("../assembly/common-resources/etc/examples/opennms.properties.d/timeseries.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            return prop;
        }
    }

    @AfterClass
    public static void afterAll() {
        if (environment != null) {
            environment.stop();
        }
    }
}
