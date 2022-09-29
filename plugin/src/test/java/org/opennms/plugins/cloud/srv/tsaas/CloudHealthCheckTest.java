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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.AUTHENTCATED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.CONFIGURED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.FAILED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.NOT_ATTEMPTED;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.integration.api.v1.health.Context;
import org.opennms.integration.api.v1.health.Response;
import org.opennms.integration.api.v1.health.Status;
import org.opennms.integration.api.v1.timeseries.InMemoryStorage;
import org.opennms.plugins.cloud.config.ConfigurationManager;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.testserver.GrpcTestServer;
import org.opennms.plugins.cloud.testserver.GrpcTestServerInterceptor;
import org.opennms.tsaas.Tsaas;

public class CloudHealthCheckTest {
    private TsaasStorage storage;
    private GrpcTestServer server;

    private ConfigurationManager cm;

    @Before
    public void setUp() throws Exception {
        GrpcConnectionConfig serverConfig = GrpcConnectionConfig.builder()
                .port(0)
                .build();

        server = new GrpcTestServer(serverConfig, new GrpcTestServerInterceptor(), new InMemoryStorage());
        server.startServer();

        GrpcConnectionConfig clientConfig = server.getConfig();

        TsaasConfig tsaasConfig = TsaasConfig.builder()
                .batchSize(1) // set to 1 so that samples are not held back in the queue
                .build();

        storage = new TsaasStorage(tsaasConfig);
        storage.initGrpc(clientConfig);
        cm = mock(ConfigurationManager.class);
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

    @Test
    public void shouldReturnStatusForTsaasSuccess() throws Exception {
        when(cm.getStatus()).thenReturn(CONFIGURED);
        assertEquals(Tsaas.CheckHealthResponse.ServingStatus.SERVING, storage.checkHealth().getStatus());
        Response response = new CloudHealthCheck(cm, this.storage).perform(mock(Context.class));
        assertEquals(Status.Success, response.getStatus());
        assertTrue(response.getMessage().contains(Tsaas.CheckHealthResponse.ServingStatus.SERVING.name()));
    }

    @Test
    public void shouldReturnStatusForTsaasFailure() throws Exception {
        when(cm.getStatus()).thenReturn(CONFIGURED);
        TsaasStorage tsaas = mock(TsaasStorage.class);
        when(tsaas.checkHealth()).thenReturn(Tsaas.CheckHealthResponse.newBuilder().setStatus(Tsaas.CheckHealthResponse.ServingStatus.NOT_SERVING).build());
        Response response = new CloudHealthCheck(cm, tsaas).perform(mock(Context.class));
        assertEquals(Status.Failure, response.getStatus());
        assertTrue(response.getMessage().contains(Tsaas.CheckHealthResponse.ServingStatus.SERVING.name()));
    }

    @Test
    public void shouldReturnStatusForInitNotAttempted() throws Exception {
        when(cm.getStatus()).thenReturn(NOT_ATTEMPTED);
        TsaasStorage tsaas = mock(TsaasStorage.class);
        Response response = new CloudHealthCheck(cm, tsaas).perform(mock(Context.class));
        assertEquals(Status.Success, response.getStatus());
        assertTrue(response.getMessage().contains(NOT_ATTEMPTED.name()));
    }

    @Test
    public void shouldReturnStatusForInitFailed() throws Exception {
        when(cm.getStatus()).thenReturn(FAILED);
        TsaasStorage tsaas = mock(TsaasStorage.class);
        Response response = new CloudHealthCheck(cm, tsaas).perform(mock(Context.class));
        assertEquals(Status.Failure, response.getStatus());
        assertTrue(response.getMessage().contains(FAILED.name()));
    }

    @Test
    public void shouldReturnStatusForInitSuccessful() throws Exception {
        when(cm.getStatus()).thenReturn(AUTHENTCATED);
        TsaasStorage tsaas = mock(TsaasStorage.class);
        Response response = new CloudHealthCheck(cm, tsaas).perform(mock(Context.class));
        assertEquals(Status.Starting, response.getStatus());
        assertTrue(response.getMessage().contains(AUTHENTCATED.name()));
    }
}
