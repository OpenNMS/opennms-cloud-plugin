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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opennms.plugins.cloud.config.ConfigStore.Key.grpchost;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.AUTHENTCATED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.CONFIGURED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.FAILED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.NOT_ATTEMPTED;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.integration.api.v1.runtime.RuntimeInfo;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.plugins.cloud.srv.RegistrationManager;
import org.opennms.plugins.cloud.srv.tsaas.TsaasStorage;
import org.opennms.plugins.cloud.testserver.GrpcTestServer;
import org.opennms.plugins.cloud.testserver.GrpcTestServerInterceptor;
import org.opennms.tsaas.Tsaas;

public class ConfigurationManagerTest {

  private GrpcTestServer server;
  private TimeSeriesStorage serverStorage;
  private GrpcConnectionConfig clientConfig;

  ConfigStore config;
  GrpcService grpc;
  RuntimeInfo info;

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

    config = new InMemoryConfigStore();
    grpc = mock(GrpcService.class);
    info = mock(RuntimeInfo.class);
    when(info.getSystemId()).thenReturn(UUID.randomUUID().toString());
  }

  @After
  public void tearDown() {
    if(server !=null) {
      server.stopServer();
    }
  }

  @Test
  public void shouldGetCloudConfig() throws InterruptedException {
    TsaasStorage grpc = mock(TsaasStorage.class);
    when(grpc.checkHealth()).thenReturn(Tsaas.CheckHealthResponse.newBuilder().setStatus(Tsaas.CheckHealthResponse.ServingStatus.SERVING).build());
    ConfigurationManager cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
            info,
            Collections.singletonList(grpc));
    assertEquals(NOT_ATTEMPTED, cm.getStatus());
    cm.initConfiguration("something");
    assertEquals(AUTHENTCATED, cm.getStatus());
    cm.configure();
    assertEquals(CONFIGURED, cm.getStatus());
    verify(grpc, times(1)).initGrpc(any());
    assertTrue(config.getOrNull(ConfigStore.Key.privatekey).startsWith("-----BEGIN PRIVATE KEY-----"));
    assertTrue(config.getOrNull(ConfigStore.Key.publickey).startsWith("-----BEGIN CERTIFICATE-----"));
  }
  @Test
  public void shouldSetStatusForFailedConfig() {
    ConfigurationManager cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
            info,
            Collections.singletonList(grpc));
    final String key = null; // will fail
    assertThrows(NullPointerException.class, () -> cm.initConfiguration(key));
    assertEquals(FAILED, cm.getStatus());
  }

  @Test
  public void shouldSetStatusForFailedInit() throws InterruptedException {
    ConfigurationManager cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
            info,
            Collections.singletonList(grpc));
    cm.initConfiguration("something");
    config.putProperty(grpchost, "I don't exist");
    assertEquals(FAILED, cm.configure());
  }

  @Test
  public void shouldCatchAllErrorsFromGrpcInit() {
    doThrow(new RuntimeException("failed")).when(grpc).initGrpc(any(GrpcConnectionConfig.class));
    ConfigurationManager cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
            info,
            Collections.singletonList(grpc));
    cm.initGrpcServices(GrpcConnectionConfig.builder().build()); // should swallow exception
  }

  @Test
  public void shouldRenewCredentials() throws Exception {
    TsaasStorage grpc = mock(TsaasStorage.class);
    when(grpc.checkHealth()).thenReturn(Tsaas.CheckHealthResponse.newBuilder().setStatus(Tsaas.CheckHealthResponse.ServingStatus.SERVING).build());
    ConfigurationManager cm = new ConfigurationManager(scv, clientConfig, clientConfig, mock(RegistrationManager.class),
            info,
            Collections.singletonList(grpc));
    assertEquals(NOT_ATTEMPTED, cm.getStatus());
    cm.initConfiguration("something");
    assertEquals(AUTHENTCATED, cm.getStatus());
    cm.configure();
    assertEquals(CONFIGURED, cm.getStatus());
    verify(grpc, times(1)).initGrpc(any());

    cm.renewCerts();
    assertTrue(scvUtil.getOrNull(SecureCredentialsVaultUtil.Type.privatekey).startsWith("-----BEGIN PRIVATE KEY-----"));
    assertTrue(scvUtil.getOrNull(SecureCredentialsVaultUtil.Type.publickey).startsWith("-----BEGIN CERTIFICATE-----"));
    assertEquals(CONFIGURED, cm.getStatus());
  }

  @Test
  public void shouldRenewCredentialsFail() {
    TsaasStorage grpc = mock(TsaasStorage.class);
    when(grpc.checkHealth()).thenReturn(Tsaas.CheckHealthResponse.newBuilder().setStatus(Tsaas.CheckHealthResponse.ServingStatus.SERVING).build());
    ConfigurationManager cm = new ConfigurationManager(scv, clientConfig, clientConfig, mock(RegistrationManager.class),
            info,
            Collections.singletonList(grpc));
    assertThrows(NullPointerException.class, cm::renewCerts); // this will fail because cm was never initialized and configured
    assertEquals(FAILED, cm.getStatus()); // make sure the status is correct
  }
}