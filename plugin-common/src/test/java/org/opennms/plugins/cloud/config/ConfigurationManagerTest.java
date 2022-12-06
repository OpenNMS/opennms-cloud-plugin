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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opennms.plugins.cloud.config.ConfigStore.Key.grpchost;
import static org.opennms.plugins.cloud.config.ConfigStore.Key.truststore;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.AUTHENTCATED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.CONFIGURED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.DEACTIVATED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.FAILED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.NOT_ATTEMPTED;
import static org.opennms.plugins.cloud.testserver.FileUtil.classpathFileToString;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.grpc.StatusRuntimeException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.opennms.integration.api.v1.runtime.RuntimeInfo;
import org.opennms.plugins.cloud.config.ConfigStore.Key;
import org.opennms.plugins.cloud.grpc.CloudLogService;
import org.opennms.plugins.cloud.grpc.CloudLogServiceConfig;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.grpc.GrpcExecutionHandler;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.plugins.cloud.srv.RegistrationManager;
import org.opennms.plugins.cloud.srv.tsaas.TsaasConfig;
import org.opennms.plugins.cloud.srv.tsaas.TsaasStorage;
import org.opennms.plugins.cloud.testserver.MockCloud;
import org.opennms.tsaas.Tsaas;

public class ConfigurationManagerTest {

    @ClassRule
    public static MockCloud cloud = MockCloud
            .builder()
            .build();

    private ConfigStore config;
    private GrpcService grpc;
    private RuntimeInfo info;
    private GrpcConnectionConfig clientConfig;
    private CloudLogServiceConfig cloudLogServiceConfig;

    @Before
    public void setUp() throws IOException {
        config = new InMemoryConfigStore();
        grpc = mock(GrpcService.class);
        info = mock(RuntimeInfo.class);
        when(info.getSystemId()).thenReturn(UUID.randomUUID().toString());
        clientConfig = cloud.getClientConfig();
        this.cloudLogServiceConfig = new CloudLogServiceConfig(1000, 60);
    }

    @Test
    public void shouldGetCloudConfig() {
        GrpcExecutionHandler grpcHandler = new GrpcExecutionHandler(new CloudLogService(cloudLogServiceConfig));
        TsaasStorage grpc = spy(new TsaasStorage(new TsaasConfig(1, 1), grpcHandler));
        ConfigurationManager cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
                info,
                Collections.singletonList(grpc));
        assertEquals(NOT_ATTEMPTED, cm.getStatus());
        cm.initConfiguration("something");
        assertEquals(AUTHENTCATED, cm.getStatus());
        config.putProperty(truststore, classpathFileToString("/cert/clienttruststore.pem"));
        cm.configure();
        assertEquals(CONFIGURED, cm.getStatus());
        verify(grpc, times(1)).initGrpc(any());
        assertTrue(config.getOrNull(ConfigStore.Key.privatekey).startsWith("-----BEGIN PRIVATE KEY-----"));
        assertTrue(config.getOrNull(ConfigStore.Key.publickey).startsWith("-----BEGIN CERTIFICATE-----"));
        cm.checkConnection();
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
        ConfigurationManager cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
                info,
                Collections.singletonList(grpc));
        assertEquals(NOT_ATTEMPTED, cm.getStatus());
        cm.initConfiguration("something");
        assertEquals(AUTHENTCATED, cm.getStatus());
        cm.configure();
        assertEquals(CONFIGURED, cm.getStatus());
        verify(grpc, times(1)).initGrpc(any());

        cm.renewCerts();
        assertTrue(config.getOrNull(Key.privatekey).startsWith("-----BEGIN PRIVATE KEY-----"));
        assertTrue(config.getOrNull(Key.publickey).startsWith("-----BEGIN CERTIFICATE-----"));
        assertEquals(CONFIGURED, cm.getStatus());
    }

    @Test
    public void shouldRenewCredentialsFail() {
        TsaasStorage grpc = mock(TsaasStorage.class);
        when(grpc.checkHealth()).thenReturn(Tsaas.CheckHealthResponse.newBuilder().setStatus(Tsaas.CheckHealthResponse.ServingStatus.SERVING).build());
        ConfigurationManager cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
                info,
                Collections.singletonList(grpc));
        assertThrows(NullPointerException.class, cm::renewCerts); // this will fail because cm was never initialized and configured
        assertEquals(FAILED, cm.getStatus()); // make sure the status is correct
    }

    /**
     * We expect the ConfigurationManager to start configuration() immediately after it was created if the status is
     * already AUTHENTCATED or CONFIGURED.
     */
    @Test
    public void shouldCallConfigureIfAuthenticatedOrConfigured() {
        // test prep: initialize already
        GrpcExecutionHandler grpcHandler = new GrpcExecutionHandler(new CloudLogService(cloudLogServiceConfig));
        TsaasStorage grpc = spy(new TsaasStorage(new TsaasConfig(1, 1), grpcHandler));
        ConfigurationManager cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
                info,
                Collections.singletonList(grpc));
        cm.initConfiguration("something");
        assertEquals(AUTHENTCATED, cm.getStatus());
        config.putProperty(truststore, classpathFileToString("/cert/clienttruststore.pem"));

        // test part 1: create new config manager => should automatically call configure() because status = AUTHENTCATED
        cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
                info,
                Collections.singletonList(grpc));
        assertEquals(CONFIGURED, cm.getStatus());
        verify(grpc, times(1)).initGrpc(any()); // this is done in configure()
        clearInvocations(grpc);

        // test part 2 : create new config manager => should automatically call configure() because status = CONFIGURED
        cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
                info,
                Collections.singletonList(grpc));
        assertEquals(CONFIGURED, cm.getStatus());
        verify(grpc, times(1)).initGrpc(any()); // this is done in configure()
    }

    @Test
    public void shouldAbleToDeactivate() {
        GrpcExecutionHandler grpcHandler = new GrpcExecutionHandler(new CloudLogService(cloudLogServiceConfig));
        TsaasStorage grpc = spy(new TsaasStorage(new TsaasConfig(1, 1), grpcHandler));
        List<GrpcService> serviceList = Collections.singletonList(grpc);
        ConfigurationManager cm = new ConfigurationManager(config, clientConfig, clientConfig, mock(RegistrationManager.class),
                info,
                serviceList);
        cm.initConfiguration("something");
        cm.configure();
        // should be normal
        cm.checkConnection();

        cm.deactivateKeyConfiguration();

        assertEquals(DEACTIVATED, cm.getStatus());
        assertThrows(StatusRuntimeException.class, () -> cm.checkConnection());
    }
}
