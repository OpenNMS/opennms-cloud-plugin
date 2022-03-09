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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opennms.integration.api.v1.timeseries.AbstractStorageIntegrationTest;
import org.opennms.integration.api.v1.timeseries.InMemoryStorage;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.tsaas.testserver.TimeseriesGrpcImpl;
import org.opennms.plugins.cloud.tsaas.testserver.TsaasServer;
import org.opennms.plugins.cloud.tsaas.testserver.TsassServerInterceptor;

public class TsaasStorageTest extends AbstractStorageIntegrationTest {

  private final static String HOST = "localhost";
  private final static int PORT = 5001;
  private final static String CLIENT_TOKEN = "my-client";

  private TsaasStorage storage;
  private TsaasServer server;

  @BeforeEach
  public void setUp() throws Exception {

    storage = new TsaasStorage(HOST, PORT, CLIENT_TOKEN);

    TimeseriesGrpcImpl timeseriesService = new TimeseriesGrpcImpl(new InMemoryStorage());
    server = new TsaasServer(timeseriesService, PORT, new TsassServerInterceptor());
    server.startServer();
    super.setUp();
  }

  @AfterEach
  public void tearDown() {
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
  @Disabled
  @Override
  public void shouldDeleteMetrics() throws Exception {
    // we don't support delete...
  }
}
