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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.integration.api.v1.timeseries.IntrinsicTagNames;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableMetric;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableSample;
import org.opennms.plugins.cloud.testserver.MockCloud;

public class TsaasStorageBatchStoringTest {

    @Rule
    public MockCloud cloud = MockCloud.builder()
            .serverStorage(Mockito.mock(TimeSeriesStorage.class))
            .build();

    @Test
    public void shouldSendStoreSamplesAfterWaitTime() throws StorageException, InterruptedException {
        TsaasConfig tsaasConfig = TsaasConfig.builder()
                .batchSize(10)
                .maxBatchWaitTimeInMilliSeconds(500)
                .build();
        TsaasStorage plugin = new TsaasStorage(tsaasConfig);
        plugin.initGrpc(cloud.getClientConfigWithToken());

        // store 2 samples. The batch size is 10 => should only call server when 10 samples are reached or maxBatchWaitTime has passed:
        plugin.store(createSamples());
        plugin.store(createSamples());
        verify(cloud.getServerStorage(), never()).store(any());

        // let's wait until maxBatchWaitTime time has passed
        Thread.sleep(tsaasConfig.getMaxBatchWaitTimeInMilliSeconds() + 10);
        plugin.store(createSamples());

        // we expect 3 samples, one from each call:
        verify(cloud.getServerStorage(), times(1)).store(argThat(l -> l.size() == 3));
        clearInvocations(cloud.getServerStorage());

        // do a second run to see if a second batch is  sent:
        // store 2 samples. The batch size is 10 => should only call server when 10 samples are reached or maxBatchWaitTime has passed:
        plugin.store(createSamples());
        plugin.store(createSamples());
        verify(cloud.getServerStorage(), never()).store(any());

        // let's wait until maxBatchWaitTime time has passed
        Thread.sleep(tsaasConfig.getMaxBatchWaitTimeInMilliSeconds() + 10);
        plugin.store(createSamples());

        // we expect 3 samples, one from each call:
        verify(cloud.getServerStorage(), times(1)).store(argThat(l -> l.size() == 3));
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

}