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

package org.opennms.plugins.cloud.srv.tsaas;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opennms.plugins.cloud.srv.tsaas.SecureCredentialsVaultUtil.SCV_ALIAS;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.scv.immutables.ImmutableCredentials;
import org.opennms.integration.api.v1.timeseries.AbstractStorageIntegrationTest;
import org.opennms.integration.api.v1.timeseries.InMemoryStorage;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.config.ConfigZipExtractor;
import org.opennms.plugins.cloud.srv.tsaas.SecureCredentialsVaultUtil.Type;
import org.opennms.plugins.cloud.testserver.GrpcTestServer;
import org.opennms.plugins.cloud.testserver.GrpcTestServerInterceptor;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

public class TsaasStorageWithMtlsTest extends AbstractStorageIntegrationTest {

  private TsaasStorage storage;
  private GrpcTestServer server;

  @Before
  public void setUp() throws Exception {
    TsaasConfig serverConfig = TsaasConfig.builder()
        .mtlsEnabled(true)
        .batchSize(1) // set to 1 so that samples are not held back in the queue
        .port(0)
        .build();

    server = new GrpcTestServer(serverConfig, new GrpcTestServerInterceptor(), new InMemoryStorage());
    server.startServer();

    TsaasConfig clientConfig = server.getConfig();

    Path pathToZipFile = Path.of("src/test/resources/cert/cloud-credentials.zip");
    assertTrue(Files.exists(pathToZipFile));
    ConfigZipExtractor certs = new ConfigZipExtractor(pathToZipFile);
    Map<String, String> attributes = new HashMap<>();
    attributes.put(Type.publickey.name(), certs.getPublicKey());
    attributes.put(Type.privatekey.name(), certs.getPrivateKey());
    // we have self signed certs, so we do need to provide a trust store with our ca:
    attributes.put(Type.truststore.name(), getCert("clienttruststore.pem"));

    SecureCredentialsVault scv = mock(SecureCredentialsVault.class);
    when(scv.getCredentials(SCV_ALIAS)).thenReturn(new ImmutableCredentials("", "", attributes));

    storage = new TsaasStorage(clientConfig, scv);
    super.setUp();
  }

  private String getCert(final String filename) throws IOException {
    return CharStreams.toString(new InputStreamReader(
        this.getClass().getResourceAsStream("/cert/" + filename), Charsets.UTF_8));
  }

  @After
  public void tearDown() throws InterruptedException {
    if (storage != null) {
      storage.destroy();
    }
    if(server !=null) {
      server.stopServer();
    }
  }

  @Override
  protected TimeSeriesStorage createStorage() throws Exception {
    return storage;
  }

  @Test
  @Ignore("we don't implement delete(), hence @Ignore")
  @Override
  public void shouldDeleteMetrics() throws Exception {
    // we don't support delete...
  }
}
