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

import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.timeseries.AbstractStorageIntegrationTest;
import org.opennms.integration.api.v1.timeseries.InMemoryStorage;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.tsaas.testserver.TsaasServer;
import org.opennms.plugins.cloud.tsaas.testserver.TsassServerInterceptor;

public class TsaasStorageTest extends AbstractStorageIntegrationTest {

  private TsaasStorage storage;
  private TsaasServer server;

  @Before
  public void setUp() throws Exception {
    TsaasConfig config = TsaasConfig.builder()
        .batchSize(1) // set to 1 so that samples are not held back in the queue
        .build();
    storage = new TsaasStorage(config, mock(SecureCredentialsVault.class));
    server = new TsaasServer(config, new TsassServerInterceptor(), new InMemoryStorage());
    server.startServer();
    super.setUp();
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
}
