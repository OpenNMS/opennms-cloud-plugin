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

package org.opennms.plugins.cloud.grpc;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.opennms.plugins.cloud.grpc.LogEntryUtil.convertToLatencyTraceList;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.plugins.cloud.util.RunnerWrapper;
import org.opennms.tsaas.telemetry.GatewayGrpc;
import org.opennms.tsaas.telemetry.GatewayOuterClass;

import com.google.common.annotations.VisibleForTesting;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Logs Requests and sends them in batches to the cloud.
 */
@Slf4j
public class CloudLogService implements RunnerWrapper, GrpcService {

    private static final int RUNNING_PERIOD = 1;
    private static final TimeUnit TIME_UNIT = MINUTES;
    private static final int GRPC_BATCH_SIZE = 1000;

    private final ScheduledExecutorService executor;

    private final GrpcExecutionHandler grpcHandler;

    @Getter
    @VisibleForTesting
    private GrpcConnection<GatewayGrpc.GatewayBlockingStub> grpc;

    private final GrpcLogEntryQueue grpcLogEntryQueue;

    public CloudLogService(ScheduledExecutorService executor, GrpcLogEntryQueue grpcLogEntryQueue, GrpcExecutionHandler grpcHandler) {
        this.executor = executor;
        this.grpcLogEntryQueue = grpcLogEntryQueue;
        this.grpcHandler = requireNonNull(grpcHandler);
        initJob();
    }

    private void initJob() {
        executor.scheduleAtFixedRate(() -> wrap(this::handleLogQueue), 1, RUNNING_PERIOD, TIME_UNIT);
    }

    @Override
    public void initGrpc(GrpcConnectionConfig grpcConfig) {
        GrpcConnection<GatewayGrpc.GatewayBlockingStub> oldGrpc = this.grpc;
        LOG.debug("Initializing Grpc Connection with host {} and port {}", grpcConfig.getHost(), grpcConfig.getPort());
        this.grpc = new GrpcConnection<>(grpcConfig, GatewayGrpc::newBlockingStub);
        CloseUtil.close(oldGrpc);
    }

    public void handleLogQueue() throws StorageException {
        if (grpcLogEntryQueue.isQueueEmpty()) {
            log.debug("The logs queue is empty, nothing to report.");
        } else {
            while (grpcLogEntryQueue.isQueueNotEmpty()) {
                List<LogEntry> logEntryList = grpcLogEntryQueue.getQueueBatch(GRPC_BATCH_SIZE);
                LOG.debug("Sending {} batch of elements to grpc endpoint", GRPC_BATCH_SIZE);
                GatewayOuterClass.SendTracesRequest sendTracesRequest = GatewayOuterClass.SendTracesRequest.newBuilder()
                        .addAllLatencyTraces(convertToLatencyTraceList(logEntryList))
                        .build();
                grpcHandler.executeRpcCallVoid(GrpcExecutionHandler.GrpcCall.builder()
                        .callToExecute(() -> this.grpc.get().sendTraces(sendTracesRequest))
                        .methodDescriptor(GatewayGrpc.getSendTracesMethod())
                        .build());

                grpcLogEntryQueue.removeBatch(logEntryList);
            }
        }
    }
}
