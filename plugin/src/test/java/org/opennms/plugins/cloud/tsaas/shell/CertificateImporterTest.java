package org.opennms.plugins.cloud.tsaas.shell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.Type;

public class CertificateImporterTest {

  @Test
  public void shouldImportCertificate() throws Exception {
    CertificateImporter importer = new CertificateImporter();
    importer.scv = mock(SecureCredentialsVault.class);
    importer.type = Type.publickey.name();
    importer.fileParam = "src/test/resources/cert/client.crt";
    importer.execute();

    ArgumentCaptor<String> aliasCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Credentials> credentialsCaptor = ArgumentCaptor.forClass(Credentials.class);
    verify(importer.scv).setCredentials(aliasCaptor.capture(), credentialsCaptor.capture());
    assertEquals(SecureCredentialsVaultUtil.SCV_ALIAS, aliasCaptor.getValue());
    Credentials credentials = credentialsCaptor.getValue();
    assertNotNull(credentials);
    String cert = credentials.getAttribute(importer.type);
    assertNotNull(cert);
    assertTrue(cert.startsWith("-----BEGIN CERTIFICATE-----"));

  }
}