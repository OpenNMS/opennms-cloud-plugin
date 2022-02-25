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

package org.opennms.plugins.cloud.tsaas.testserver;

import static org.awaitility.Awaitility.await;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.opennms.plugins.cloud.tsaas.grpc.comp.ZStdCodecRegisterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of the server side of the grpc channel.
 * It is backed by an InMemoryStorage.
 * We use it to verify that our grpc implementation works.
 */
public class TsaasServer {

    private static final Logger LOG = LoggerFactory.getLogger(TsaasServer.class);

    private final int port;
    private Server server;
    private TimeseriesGrpcImpl timeseriesService;
    private TsassServerInterceptor serverInterceptor;


    public TsaasServer(final TimeseriesGrpcImpl timeseriesService,
                       Integer port, TsassServerInterceptor serverInterceptor) {
        this.timeseriesService = Objects.requireNonNull(timeseriesService);
        this.port = Objects.requireNonNull(port);
        this.serverInterceptor = serverInterceptor;
    }

    @PostConstruct
    public void startServer() {
        try {
            server = NettyServerBuilder
                    .forPort(port)
                    .addService(timeseriesService)
                    .decompressorRegistry(ZStdCodecRegisterUtil.createDecompressorRegistry())
                    .compressorRegistry(ZStdCodecRegisterUtil.createCompressorRegistry())
                    .intercept(serverInterceptor)
                    .build()
                    .start();
            LOG.info("Grpc Server started, listening on {}", port);
            CompletableFuture.runAsync(() -> {
                try {
                    server.awaitTermination();
                    this.timeseriesService.shutdown();
                } catch (InterruptedException ex) {
                    LOG.error(ex.getMessage(), ex);
                }
            });
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    @PreDestroy
    public void stopServer() {
        if (server != null) {
            server.shutdown();
            await().until(() -> server.isShutdown());
        }
        LOG.info("Grpc Server stopped");
    }

}
