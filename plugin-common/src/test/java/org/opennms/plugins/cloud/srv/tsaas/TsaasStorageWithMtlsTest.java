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

import static org.opennms.plugins.cloud.testserver.FileUtil.classpathFileToString;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.opennms.integration.api.v1.timeseries.AbstractStorageIntegrationTest;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.testserver.MockCloud;

public class TsaasStorageWithMtlsTest extends AbstractStorageIntegrationTest {

  private TsaasStorage storage;
  @Rule
  public final MockCloud cloud = MockCloud.builder()
          .serverConfig(MockCloud
                  .defaultServerConfig()
                  .security(GrpcConnectionConfig.Security.MTLS)
                  .build())
          .build();

  @Before
  public void setUp() throws Exception {
    GrpcConnectionConfig.GrpcConnectionConfigBuilder clientConfig = cloud.getClientConfigWithToken().toBuilder();
    Map<String, String> attributes = new HashMap<>();
    clientConfig.publicKey(classpathFileToString("/cert/client_cert.crt"));
    clientConfig.privateKey(classpathFileToString("/cert/client_private_key.key"));
    // we have self signed certs, so we do need to provide a trust store with our ca:
    clientConfig.clientTrustStore(classpathFileToString("/cert/clienttruststore.pem"));

    TsaasConfig tsaasConfig = TsaasConfig.builder().batchSize(1).build(); // set to 1 so that samples are not held back in the queue
    storage = new TsaasStorage(tsaasConfig);
    storage.initGrpc(clientConfig.build());
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
}
