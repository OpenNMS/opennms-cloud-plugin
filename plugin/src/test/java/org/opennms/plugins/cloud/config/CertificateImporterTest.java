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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.After;
import org.junit.Test;
import org.opennms.plugins.cloud.config.ConfigStore.Key;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;

public class CertificateImporterTest {

  private Path credentialsFile;

  @Test
  public void shouldRejectMissingFiles() {
    credentialsFile = Path.of("IDontExist.file");
    final String file = credentialsFile.toString();
    final ConfigStore scv = new InMemoryConfigStore();
    final GrpcConnectionConfig config = GrpcConnectionConfig.builder().build();
    final CertificateImporter importer = new CertificateImporter(file, scv, config);
    assertThrows(IllegalArgumentException.class, importer::doIt);
  }

  @Test
  public void shouldImportCertificates() throws Exception {
    ConfigStore config = new InMemoryConfigStore();
    credentialsFile = Files.createTempFile(this.getClass().getSimpleName(), ".zip");
    Files.copy(Path.of("src/test/resources/cert/cloud-credentials.zip"), credentialsFile, StandardCopyOption.REPLACE_EXISTING);
    assertTrue(Files.exists(credentialsFile));
    CertificateImporter importer = new CertificateImporter(credentialsFile.toString(),
            config, GrpcConnectionConfig.builder().build());
    importer.doIt();

    String value = config.getOrNull(Key.publickey);
    assertNotNull(value);
    assertTrue(value.startsWith("-----BEGIN CERTIFICATE-----"));
    value = config.getOrNull(ConfigStore.Key.privatekey);
    assertNotNull(value);
    assertTrue(value.startsWith("-----BEGIN PRIVATE KEY-----"));
    value = config.getOrNull(ConfigStore.Key.tokenvalue);
    assertNotNull(value);
    assertTrue(value.startsWith("acme"));

    // Check if file has been deleted. we expect that the file is deleted after a successful import:
    assertFalse(Files.exists(credentialsFile));
  }

  @Test
  public void shouldNotDeleteIfScvStorageGoesWrong() throws Exception {
    ConfigStore config = mock(ConfigStore.class); // won't store anything => storage will go wrong
    credentialsFile = Files.createTempFile(this.getClass().getSimpleName(), ".zip");
    Files.copy(Path.of("src/test/resources/cert/cloud-credentials.zip"), credentialsFile, StandardCopyOption.REPLACE_EXISTING);
    assertTrue(Files.exists(credentialsFile));
    CertificateImporter importer = new CertificateImporter(credentialsFile.toString(),
            config,
            GrpcConnectionConfig.builder().build());
    importer.doIt();

    // Check if file has been deleted. We expect that the file is not deleted after an unsuccessful import:
    assertTrue(Files.exists(credentialsFile));
  }

  @After
  public void tearDown() throws IOException {
    Files.deleteIfExists(this.credentialsFile);
  }

}