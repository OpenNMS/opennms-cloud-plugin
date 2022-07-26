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

package org.opennms.plugins.cloud.tsaas.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.SCV_ALIAS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.After;
import org.junit.Test;
import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.Type;
import org.opennms.plugins.cloud.tsaas.TsaasConfig;

public class CertificateImporterTest {

  private Path credentialsFile;

  @Test
  public void shouldRejectMissingFiles() throws Exception {
    credentialsFile = Path.of("IDontExist.file");
    final String file = credentialsFile.toString();
    final SecureCredentialsVault scv = new InMemoryScv();
    final TsaasConfig config = TsaasConfig.builder().build();
    final CertificateImporter importer = new CertificateImporter(file, scv, config);
    assertThrows(IllegalArgumentException.class, importer::doIt);
  }

  @Test
  public void shouldImportCertificates() throws Exception {
    SecureCredentialsVault scv = new InMemoryScv();
    credentialsFile = Files.createTempFile(this.getClass().getSimpleName(), ".zip");
    Files.copy(Path.of("src/test/resources/cert/cloud-credentials.zip"), credentialsFile, StandardCopyOption.REPLACE_EXISTING);
    assertTrue(Files.exists(credentialsFile));
    CertificateImporter importer = new CertificateImporter(credentialsFile.toString(), scv, TsaasConfig.builder().build());
    importer.doIt();

    Credentials credentials = scv.getCredentials(SCV_ALIAS);
    assertNotNull(credentials);
    String value = credentials.getAttribute(Type.publickey.name());
    assertNotNull(value);
    assertTrue(value.startsWith("-----BEGIN CERTIFICATE-----"));
    value = credentials.getAttribute(Type.privatekey.name());
    assertNotNull(value);
    assertTrue(value.startsWith("-----BEGIN PRIVATE KEY-----"));
    value = credentials.getAttribute(Type.token.name());
    assertNotNull(value);
    assertTrue(value.startsWith("acme"));

    // Check if file has been deleted. we expect that the file is deleted after a successful import:
    assertFalse(Files.exists(credentialsFile));
  }

  @Test
  public void shouldNotDeleteIfScvStorageGoesWrong() throws Exception {
    SecureCredentialsVault scv = mock(SecureCredentialsVault.class);
    when(scv.getCredentials(any())).thenReturn(mock(Credentials.class)); // won't store anything => storage will go wrong
    credentialsFile = Files.createTempFile(this.getClass().getSimpleName(), ".zip");
    Files.copy(Path.of("src/test/resources/cert/cloud-credentials.zip"), credentialsFile, StandardCopyOption.REPLACE_EXISTING);
    assertTrue(Files.exists(credentialsFile));
    CertificateImporter importer = new CertificateImporter(credentialsFile.toString(), scv, TsaasConfig.builder().build());
    importer.doIt();

    // Check if file has been deleted. We expect that the file is not deleted after an unsuccessful import:
    assertTrue(Files.exists(credentialsFile));
  }

  @After
  public void tearDown() throws IOException {
    Files.deleteIfExists(this.credentialsFile);
  }

}