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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.integration.api.v1.timeseries.AbstractStorageIntegrationTest;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.grpc.CloudLogServiceConfig;
import org.opennms.plugins.cloud.grpc.CloudLogServiceTestUtil;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.grpc.GrpcExecutionHandler;
import org.opennms.plugins.cloud.grpc.CloudLogService;
import org.opennms.plugins.cloud.testserver.LogServiceGrpc;
import org.opennms.plugins.cloud.testserver.MockCloud;
import org.opennms.tsaas.Tsaas;
import org.opennms.tsaas.telemetry.GatewayOuterClass;

public class TsaasStorageTest extends AbstractStorageIntegrationTest implements CloudLogServiceTestUtil {

    @Rule
    public final MockCloud cloud = MockCloud.builder()
            .build();

    private TsaasStorage storage;

    private CloudLogService cloudLogService;

    @Before
    public void setUp() throws Exception {
        GrpcConnectionConfig clientConfig = cloud.getClientConfigWithToken();
        CloudLogServiceConfig cloudLogServiceConfig = new CloudLogServiceConfig(1000, 60);
        cloudLogService = new CloudLogService(cloudLogServiceConfig);
        GrpcExecutionHandler grpcExecutionHandler = new GrpcExecutionHandler(cloudLogService);
        TsaasConfig tsaasConfig = TsaasConfig.builder()
                .batchSize(1) // set to 1 so that samples are not held back in the queue
                .build();
        storage = new TsaasStorage(tsaasConfig, grpcExecutionHandler);
        storage.initGrpc(clientConfig);
        cloudLogService.initGrpc(clientConfig);
        super.setUp();
    }

    @After
    public void tearDown() {
        if (storage != null) {
            storage.destroy();
        }
    }

    @Override
    protected TimeSeriesStorage createStorage() {
        return storage;
    }

    @Ignore("we don't implement delete(), hence @Ignore")
    @Override
    public void shouldDeleteMetrics() {
        // we don't support delete...
    }

    @Test
    public void shouldReturnHealthStatus() {
        Tsaas.CheckHealthResponse health = this.storage.checkHealth();
        assertNotNull(health);
        assertEquals(Tsaas.CheckHealthResponse.ServingStatus.SERVING, health.getStatus());
    }

    @Test
    public void log_service_grpc_should_receive_expected_number_of_entries() throws StorageException {
        // Given
        cloudLogService.deleteAll();
        fillOutLogEntryQueueCloudLog(5, cloudLogService);

        // When
        cloudLogService.handleLogQueue();

        // Then
        List<GatewayOuterClass.LatencyTrace> latencyTraceList = LogServiceGrpc.getList();
        assertEquals(5, latencyTraceList.size());
        latencyTraceList.forEach(latencyTrace -> assertNotNull(latencyTrace.getTraceId()));
    }
}
