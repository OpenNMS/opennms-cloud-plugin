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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Avoid using parameter testing for easier debug
 * Testing flow:
 * 1. copy packages from assembly
 * 2. docker-compose build (Dockerfile install all needed packages)
 * 3. start opennms & sentinel container
 * 4. connect to karaf shell and check plugin status (opennms should start by default, sentinel need to install)
 */
public class InstallPackageTest {
    private static final Logger LOG = LoggerFactory.getLogger(InstallPackageTest.class);

    public static DockerComposeContainer<?> environment;

    private KarafShell opennmsShell;
    private KarafShell sentinelShell;

    @BeforeClass
    public void copyAllPackages() throws IOException {
        // deb
        copyPackages("src/test/resources/package/deb/opennms-plugin-cloud.deb",
                "../assembly/opennms-deb/target", "opennms-plugin-cloud.*.deb");
        copyPackages("src/test/resources/package/deb/sentinel-plugin-cloud.deb",
                "../assembly/sentinel-deb/target", "sentinel-plugin-cloud.*.deb");

        // rpm
        copyPackages("src/test/resources/package/rpm/opennms-plugin-cloud.rpm",
                "../assembly/opennms-rpm/target/rpm/opennms-plugin-cloud/RPMS/noarch", "opennms-plugin-cloud.*.rpm");
        copyPackages("src/test/resources/package/rpm/sentinel-plugin-cloud.rpm",
                "../assembly/sentinel-rpm/target/rpm/sentinel-plugin-cloud/RPMS/noarch", "sentinel-plugin-cloud.*.rpm");
    }

    private void copyPackages(String dockerFileDir, String srcPath, String fileRegex) throws IOException {
        List<Path> files = Files.find(Path.of(srcPath),
                Integer.MAX_VALUE,
                (path, basicFileAttributes) -> path.toFile().getName().matches(fileRegex)
        ).collect(Collectors.toList());
        Assert.assertEquals(String.format("Should only fine 1 package file. Path: %s Pattern: %s", srcPath, fileRegex), 1, files.size());
        Files.copy(files.get(0), Path.of(dockerFileDir + "/opennms-plugin-cloud.deb"));
    }
    
    private void startContainer(String dockerfileDir, String opennmsHome, String sentinelHome) {
        environment = new DockerComposeContainer<>(new File("src/test/resources/package/docker-compose.yaml"))
                .withBuild(true)
                .withTailChildContainers(true)
                .withEnv("DOCKERFILE_DIR", dockerfileDir)
                .withEnv("OPENNMS_HOME", opennmsHome)
                .withEnv("SENTINEL_HOME", sentinelHome)
                .withExposedService("horizon_1", 8101, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)))
                .withExposedService("sentinel_1", 8301, Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(240)))
                .withLogConsumer("horizon_1", new Slf4jLogConsumer(LOG))
                .withLogConsumer("sentinel_1", new Slf4jLogConsumer(LOG));

        environment.start();
        printContainerStartup("horizon_1");
        printContainerStartup("sentinel_1");
        int opennmsShellPort = environment.getServicePort("horizon_1", 8101);
        this.opennmsShell = new KarafShell(InetSocketAddress.createUnresolved("localhost", opennmsShellPort));
        int sentinelShellPort = environment.getServicePort("sentinel_1", 8301);
        this.sentinelShell = new KarafShell(InetSocketAddress.createUnresolved("localhost", sentinelShellPort));
    }

    @Test
    @SuppressWarnings("java:S2699") // no assertions because we are an integration test and test against karaf shell
    public void checkDebPluginInstallAndStarted() {
        startContainer("./deb", "/usr/share/opennms", "/usr/share/sentinel");
        checkPluginLoading();
    }

    @Test
    @SuppressWarnings("java:S2699") // no assertions because we are an integration test and test against karaf shell
    public void checkRpmPluginInstallAndStarted() {
        startContainer("./rpm", "/opt/opennms", "/opt/sentinel");
        checkPluginLoading();
    }

    private void checkPluginLoading() {
        // horizon is enabled by boot
        opennmsShell.runCommand("feature:list | grep opennms-plugin-cloud-core", output -> output.contains("Started"));

        // sentinel did not auto start
        sentinelShell.runCommand("feature:install opennms-plugin-cloud-sentinel");
        sentinelShell.runCommand("feature:list | grep opennms-plugin-cloud-sentinel", output -> output.contains("Started"));
    }

    private static ContainerState getContainer(final String container) {
        return environment.getContainerByServiceName(container)
                .orElseThrow(() -> new IllegalArgumentException(String.format("container %s not found", container)));
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

    @AfterClass
    public static void afterAll() {
        if (environment != null) {
            environment.stop();
        }
    }
}
