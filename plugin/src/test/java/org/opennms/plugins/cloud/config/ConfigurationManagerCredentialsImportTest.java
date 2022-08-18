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

package org.opennms.plugins.cloud.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.opennms.plugins.cloud.config.SecureCredentialsVaultUtil.SCV_ALIAS;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.RegistrationManager;

public class ConfigurationManagerCredentialsImportTest {

    private final static String OPENNMS_HOME = "opennms.home";

    private Path openNmsHome;
    private String originalOpenNmsHome;

    @Before
    public void setUp() {
        this.originalOpenNmsHome = System.getProperty("opennms.home");
    }

    @Test
    public void shouldImportCloudCredentialsIfPresent() throws Exception {

        InMemoryScv scv = new InMemoryScv();
        ConfigurationManager registrationManager = new ConfigurationManager(scv, GrpcConnectionConfig.builder().build()
                , mock(RegistrationManager.class), new ArrayList<>());
        openNmsHome = Files.createTempDirectory(this.getClass().getSimpleName());
        System.setProperty(OPENNMS_HOME, openNmsHome.toString());
        Files.createDirectory(Path.of(openNmsHome.toString(), "etc"));
        Path credentialsFile = Path.of(openNmsHome.toString(), "etc", "cloud-credentials.zip");
        Files.copy(Path.of("src/test/resources/cert/cloud-credentials.zip"), credentialsFile);
        assertTrue(Files.exists(credentialsFile));
        registrationManager.importCloudCredentialsIfPresent();
        Credentials credentials = scv.getCredentials(SCV_ALIAS);
        assertNotNull(credentials);
        String value = credentials.getAttribute(SecureCredentialsVaultUtil.Type.publickey.name());
        assertNotNull(value);
        assertTrue(value.startsWith("-----BEGIN CERTIFICATE-----"));

        // Check if file has been deleted. we expect that the file is deleted after a successful import:
        assertFalse(Files.exists(credentialsFile));
    }

    @After
    public void tearDown() throws IOException {
        if(this.openNmsHome != null) {
            Files.walk(this.openNmsHome)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
        if (originalOpenNmsHome != null) {
            System.setProperty(OPENNMS_HOME, originalOpenNmsHome);
        }
    }
}
