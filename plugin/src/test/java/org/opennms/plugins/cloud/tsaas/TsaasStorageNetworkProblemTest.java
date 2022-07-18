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

package org.opennms.plugins.cloud.tsaas;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.timeseries.IntrinsicTagNames;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableMetric;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableSample;
import org.opennms.plugins.cloud.tsaas.testserver.TsaasServer;
import org.opennms.plugins.cloud.tsaas.testserver.TsassServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.StatusRuntimeException;

public class TsaasStorageNetworkProblemTest {

  private static final Logger LOG = LoggerFactory.getLogger(TsaasStorageNetworkProblemTest.class);

  private TsaasServer server;
  private TimeSeriesStorage serverStorage;

  @Before
  public void setUp() throws Exception {
    TsaasConfig config = TsaasConfig.builder()
        .build();
    serverStorage = Mockito.mock(TimeSeriesStorage.class);
    server = new TsaasServer(config, new TsassServerInterceptor(), serverStorage);
    server.startServer();
  }

  @After
  public void tearDown() {
    if(server !=null) {
      server.stopServer();
    }
  }

  @Test
  public void shouldRecoverAfterServerFailure() throws StorageException, InterruptedException {
    TsaasConfig config = TsaasConfig
        .builder()
        .batchSize(1)
        .build();
    TsaasStorage plugin = new TsaasStorage(config, mock(SecureCredentialsVault.class));

    plugin.store(createSamples());
    verify(serverStorage, times(1)).store(any());
    reset(serverStorage);

    server.stopServer();
    assertThrows(io.grpc.StatusRuntimeException.class, () -> plugin.store(createSamples()));

    verify(serverStorage, never()).store(any());

    server.startServer();
    Thread.sleep(1000); // wait till we are ready
    plugin.store(createSamples());
    verify(serverStorage, times(1)).store(any());
  }

  @Test
  public void shouldRecoverAfterServerException() throws StorageException, InterruptedException {
    TsaasConfig config = TsaasConfig
            .builder()
            .batchSize(1)
            .build();
    TsaasStorage plugin = new TsaasStorage(config, mock(SecureCredentialsVault.class));

    doThrow(new StorageException("hups")).when(serverStorage).store(any());
    assertThrows(io.grpc.StatusRuntimeException.class, () -> plugin.store(createSamples()));
    doNothing().when(serverStorage).store(any());
    plugin.store(createSamples());
    verify(serverStorage, times(2)).store(any());
  }


  private List<Sample> createSamples () {
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