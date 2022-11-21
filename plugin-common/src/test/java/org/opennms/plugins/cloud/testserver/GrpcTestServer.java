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

import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.grpc.comp.ZstdCodecRegisterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

/**
 * A simple implementation of the server side of the grpc channel.
 * It is backed by an InMemoryStorage.
 * We use it to verify that our grpc implementation works.
 */
public class GrpcTestServer {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcTestServer.class);

    private GrpcConnectionConfig config;
    private Server server;
    private final TsaasGrpcImpl timeSeriesService;
    private final ConfigGrpcImpl configGrpcService;
    private final LogServiceGrpc logServiceGrpc;

    public GrpcTestServer(final GrpcConnectionConfig config,
                          final TimeSeriesStorage storage) {
        this.configGrpcService = new ConfigGrpcImpl();
        this.timeSeriesService = new TsaasGrpcImpl(storage);
        this.config = config;
        this.logServiceGrpc = new LogServiceGrpc();
    }

    @PostConstruct
    public void startServer(final String certPrefix,
                            final String keyCertChainFilename,
                            final String keyFilename) throws IOException {
        NettyServerBuilder builder = NettyServerBuilder
                .forPort(config.getPort())
                .addService(configGrpcService)
                .addService(timeSeriesService)
                .addService(logServiceGrpc)
                .decompressorRegistry(ZstdCodecRegisterUtil.createDecompressorRegistry())
                .compressorRegistry(ZstdCodecRegisterUtil.createCompressorRegistry())
                .intercept(new GrpcTestServerInterceptor());
        if (GrpcConnectionConfig.Security.TLS == this.config.getSecurity()
                || GrpcConnectionConfig.Security.MTLS == this.config.getSecurity()) {
            builder.sslContext(
                    GrpcSslContexts
                            .forServer(this.getClass().getResourceAsStream(certPrefix + "/" + keyCertChainFilename), // an X.509 certificate chain file in PEM format
                                    this.getClass().getResourceAsStream(certPrefix + "/" + keyFilename))
                            .trustManager(this.getClass().getResourceAsStream(certPrefix + "/servertruststore.pem"))
                            .build());
        }
        server = builder
                .build()
                .start();

        LOG.info("Grpc Server started, listening on {}", server.getPort());
        if (server.getPort() != config.getPort()) {
            LOG.info("saving port {} into config", server.getPort());
            config = config.toBuilder().port(server.getPort()).build();
        }
        this.configGrpcService.init(config);
        CompletableFuture.runAsync(() -> {
            try {
                server.awaitTermination();
                this.timeSeriesService.shutdown();
            } catch (InterruptedException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        });
    }

    @PreDestroy
    public void stopServer() {
        if (server != null) {
            server.shutdown();
            await().until(() -> server.isShutdown());
        }
        LOG.info("Grpc Server stopped");
    }

    public GrpcConnectionConfig getConfig() {
        return config;
    }
}
