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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.integration.api.v1.timeseries.IntrinsicTagNames;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableMetric;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableSample;
import org.opennms.plugins.cloud.grpc.CloudLogService;
import org.opennms.plugins.cloud.grpc.CloudLogServiceConfig;
import org.opennms.plugins.cloud.grpc.GrpcExecutionHandler;
import org.opennms.plugins.cloud.testserver.MockCloud;

public class TsaasStorageNetworkProblemTest {

    @Rule
    public MockCloud cloud = MockCloud.builder()
            .serverStorage(mock(TimeSeriesStorage.class))
            .build();

    @Test
    public void shouldRecoverAfterServerFailure() throws StorageException, InterruptedException, IOException {
        CloudLogServiceConfig cloudLogServiceConfig = new CloudLogServiceConfig(1000, 60);
        GrpcExecutionHandler grpcHandler = new GrpcExecutionHandler(new CloudLogService(cloudLogServiceConfig));
        TsaasStorage plugin = new TsaasStorage(TsaasConfig.builder().batchSize(1).build(), grpcHandler);
        plugin.initGrpc(cloud.getClientConfigWithToken());

        plugin.store(createSamples());
        verify(cloud.getServerStorage(), times(1)).store(any());
        reset(cloud.getServerStorage());

        cloud.stop();
        assertThrows(StorageException.class, () -> plugin.store(createSamples()));

        verify(cloud.getServerStorage(), never()).store(any());

        cloud = MockCloud.builder()
                .serverConfig(cloud.getClientConfigWithToken()) // to keep same port
                .serverStorage(mock(TimeSeriesStorage.class))
                .build();
        cloud.start();
        plugin.getGrpc().managedChannel.resetConnectBackoff(); // make sure the channel is ready. Otherwise it has a short wait time
        plugin.store(createSamples());
        verify(cloud.getServerStorage(), times(1)).store(any());
    }

    @Test
    public void shouldRecoverAfterServerException() throws StorageException, InterruptedException {
        CloudLogServiceConfig cloudLogServiceConfig = new CloudLogServiceConfig(1000, 60);
        GrpcExecutionHandler grpcHandler = new GrpcExecutionHandler(new CloudLogService(cloudLogServiceConfig));
        TsaasStorage plugin = new TsaasStorage(TsaasConfig.builder().batchSize(1).build(), grpcHandler);
        plugin.initGrpc(cloud.getClientConfigWithToken());

        doThrow(new StorageException("hups")).when(cloud.getServerStorage()).store(any());
        plugin.store(createSamples()); // nothing should happen since this is a non recoverable exception
        doNothing().when(cloud.getServerStorage()).store(any());
        plugin.store(createSamples());
        verify(cloud.getServerStorage(), times(2)).store(any());
    }


    private List<Sample> createSamples() {
        return Collections.singletonList(
                ImmutableSample.builder()
                        .time(Instant.now())
                        .metric(ImmutableMetric.builder()
                                .intrinsicTag(IntrinsicTagNames.resourceId, "a")
                                .intrinsicTag(IntrinsicTagNames.name, "b")
                                .build())
                        .value(3.0)
                        .build());
    }

    @After
    public void shutDown() {
        cloud.stop();
    }

}
