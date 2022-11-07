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
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.integration.api.v1.timeseries.AbstractStorageIntegrationTest;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.grpc.GrpcExecutionHandler;
import org.opennms.plugins.cloud.grpc.GrpcLogEntryQueue;
import org.opennms.plugins.cloud.testserver.MockCloud;
import org.opennms.tsaas.Tsaas;

public class TsaasStorageTest extends AbstractStorageIntegrationTest {

    @Rule
    public final MockCloud cloud = MockCloud.builder()
            .build();

    private TsaasStorage storage;

    @Before
    public void setUp() throws Exception {
        GrpcConnectionConfig clientConfig = cloud.getClientConfigWithToken();
        GrpcLogEntryQueue grpcLogEntryQueue = new GrpcLogEntryQueue();
        GrpcExecutionHandler grpcHandler = new GrpcExecutionHandler(grpcLogEntryQueue);
        TsaasConfig tsaasConfig = TsaasConfig.builder()
                .batchSize(1) // set to 1 so that samples are not held back in the queue
                .build();
        storage = new TsaasStorage(tsaasConfig, grpcHandler);
        storage.initGrpc(clientConfig);
        super.setUp();
    }

    @After
    public void tearDown() throws InterruptedException {
        if (storage != null) {
            storage.destroy();
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

    @Test
    public void shouldReturnHealthStatus() {
        Tsaas.CheckHealthResponse health = this.storage.checkHealth();
        assertNotNull(health);
        assertEquals(Tsaas.CheckHealthResponse.ServingStatus.SERVING, health.getStatus());
    }
}
