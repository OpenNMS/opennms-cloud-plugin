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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.integration.api.v1.health.Context;
import org.opennms.integration.api.v1.health.Response;
import org.opennms.integration.api.v1.health.Status;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.timeseries.InMemoryStorage;
import org.opennms.plugins.cloud.srv.tsaas.testserver.TsaasServer;
import org.opennms.plugins.cloud.srv.tsaas.testserver.TsassServerInterceptor;
import org.opennms.tsaas.Tsaas;

public class CloudHealthCheckTest {
    private TsaasStorage storage;
    private TsaasServer server;

    @Before
    public void setUp() throws Exception {
        TsaasConfig serverConfig = TsaasConfig.builder()
                .batchSize(1) // set to 1 so that samples are not held back in the queue
                .port(0)
                .build();

        server = new TsaasServer(serverConfig, new TsassServerInterceptor(), new InMemoryStorage());
        server.startServer();

        TsaasConfig clientConfig = server.getConfig();

        storage = new TsaasStorage(clientConfig, mock(SecureCredentialsVault.class));
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
    public void shouldReturnStatus() throws Exception {
        assertEquals(Tsaas.CheckHealthResponse.ServingStatus.SERVING, storage.checkHealth().getStatus());
        Response response = new CloudHealthCheck(this.storage).perform(mock(Context.class));
        assertEquals(Status.Success, response.getStatus());
        assertTrue(response.getMessage().contains(Tsaas.CheckHealthResponse.ServingStatus.SERVING.name()));
    }
}
