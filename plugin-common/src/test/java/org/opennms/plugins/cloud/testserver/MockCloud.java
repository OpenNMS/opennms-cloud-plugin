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

package org.opennms.plugins.cloud.testserver;

import static org.opennms.plugins.cloud.testserver.FileUtil.classpathFileToString;

import java.io.IOException;
import java.util.Objects;

import org.junit.rules.ExternalResource;
import org.opennms.integration.api.v1.timeseries.InMemoryStorage;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;

import lombok.Builder;
import lombok.Getter;


/**
 * A grpc server that simulates the cloud side.
 * Can be used as @Rule in a unit test.
 * Use the builder to change its behaviour.
 */
public class MockCloud extends ExternalResource implements AutoCloseable {

    private GrpcTestServer server;
    @Getter
    private final TimeSeriesStorage serverStorage;

    private GrpcConnectionConfig serverConfig;
    @Getter
    private GrpcConnectionConfig clientConfig;

    @Builder
    public MockCloud(final GrpcConnectionConfig serverConfig,
                     final TimeSeriesStorage serverStorage) {
        this.serverConfig = serverConfig;
        this.serverStorage = Objects.requireNonNull(serverStorage);
    }

    public static GrpcConnectionConfig.GrpcConnectionConfigBuilder defaultServerConfig() {
        return GrpcConnectionConfig.builder()
                .port(0)
                .security(GrpcConnectionConfig.Security.TLS);
    }

    public void start() throws IOException {
        server = new GrpcTestServer(serverConfig, new GrpcTestServerInterceptor(), serverStorage);
        server.startServer();
        clientConfig = server
                .getConfig()
                .toBuilder()
                .clientTrustStore(classpathFileToString("/cert/clienttruststore.pem"))
                .build();
    }

    /** Simulates a config that is valid after init() and configure() (we have already received the access token). */
    public GrpcConnectionConfig getClientConfigWithToken() {
        return this.getClientConfig()
                .toBuilder()
                .tokenValue("myAccessToken")
                .build();
    }

    @Override
    protected void before() throws IOException {
        this.start();
    }

    @Override
    protected void after() {
        this.stop();
    }

    @Override // AutoClosable
    public void close() {
        this.stop();
    }

    public void stop() {
        if (server != null) {
            server.stopServer();
        }
    }


    public static class MockCloudBuilder {
        private GrpcConnectionConfig serverConfig = defaultServerConfig().build(); // default value
        private TimeSeriesStorage serverStorage = new InMemoryStorage(); //default value
    }
}
