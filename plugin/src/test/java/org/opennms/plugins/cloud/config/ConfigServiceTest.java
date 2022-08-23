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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.plugins.cloud.srv.RegistrationManager;
import org.opennms.plugins.cloud.testserver.GrpcTestServer;
import org.opennms.plugins.cloud.testserver.GrpcTestServerInterceptor;

public class ConfigServiceTest {

  private GrpcTestServer server;
  private TimeSeriesStorage serverStorage;
  private GrpcConnectionConfig clientConfig;

  @Before
  public void setUp() throws IOException {
    GrpcConnectionConfig serverConfig = GrpcConnectionConfig.builder()
            .port(0)
            .security(GrpcConnectionConfig.Security.TLS)
            .build();
    serverStorage = mock(TimeSeriesStorage.class);
    server = new GrpcTestServer(serverConfig, new GrpcTestServerInterceptor(), serverStorage);
    server.startServer();
    clientConfig = server
            .getConfig()
            .toBuilder()
            .clientTrustStore(Files.readString(Path.of("src/test/resources/cert/clienttruststore.pem")))
            .build();
  }

  @After
  public void tearDown() {
    if(server !=null) {
      server.stopServer();
    }
  }

  @Test
  public void shouldGetCloudConfig() {
    InMemoryScv scv = new InMemoryScv();
    SecureCredentialsVaultUtil scvUtil = new SecureCredentialsVaultUtil(scv);
    GrpcService grpc = mock(GrpcService.class);
    ConfigurationManager cm = new ConfigurationManager(scv, clientConfig, clientConfig, mock(RegistrationManager.class), Collections.singletonList(grpc));
    cm.initConfiguration("something");
    cm.configure();
    verify(grpc, times(1)).initGrpc(any());
    assertTrue(scvUtil.getOrNull(SecureCredentialsVaultUtil.Type.privatekey).startsWith("-----BEGIN PRIVATE KEY-----"));
    assertTrue(scvUtil.getOrNull(SecureCredentialsVaultUtil.Type.publickey).startsWith("-----BEGIN CERTIFICATE-----"));
  }

}