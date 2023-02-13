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
import static org.apache.commons.lang3.StringUtils.contains;
import static org.opennms.plugins.cloud.grpc.CloudLogServiceUtil.convertToLatencyTraceList;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.tsaas.telemetry.GatewayGrpc;
import org.opennms.tsaas.telemetry.GatewayOuterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import lombok.Getter;

public class CloudLogService implements GrpcService {

    private final ConcurrentLinkedQueue<LogEntry> logEntryQueue;

    private final CloudLogServiceConfig cloudLogServiceConfig;

    private static final String SEND_TRACES_METHOD = "Gateway/SendTraces";

    private static final Logger LOG = LoggerFactory.getLogger(CloudLogService.class);


    @Getter
    @VisibleForTesting
    private GrpcConnection<GatewayGrpc.GatewayBlockingStub> grpc;

    public CloudLogService(CloudLogServiceConfig cloudLogServiceConfig) {
        this.cloudLogServiceConfig = requireNonNull(cloudLogServiceConfig);
        logEntryQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void initGrpc(GrpcConnectionConfig grpcConfig) {
        GrpcConnection<GatewayGrpc.GatewayBlockingStub> oldGrpc = this.grpc;
        LOG.debug("Initializing Grpc Connection with host {} and port {}", grpcConfig.getHost(), grpcConfig.getPort());
        this.grpc = new GrpcConnection<>(grpcConfig, GatewayGrpc::newBlockingStub);
        CloseUtil.close(oldGrpc);
    }

    public void handleLogQueue() throws StorageException {
        GrpcExecutionHandler grpcExecutionHandler = new GrpcExecutionHandler(this);
        if (isQueueEmpty()) {
            LOG.debug("The logs queue is empty, nothing to report.");
        } else {
            while (isQueueNotEmpty()) {
                List<LogEntry> logEntryList = getQueueBatch(cloudLogServiceConfig.getBatchSize());
                LOG.debug("Sending {} batch of elements to grpc endpoint", logEntryList.size());
                GatewayOuterClass.SendTracesRequest sendTracesRequest = GatewayOuterClass.SendTracesRequest.newBuilder()
                        .addAllLatencyTraces(convertToLatencyTraceList(logEntryList))
                        .build();
                grpcExecutionHandler.executeRpcCallVoid(GrpcExecutionHandler.GrpcCall.builder()
                        .callToExecute(() -> this.getGrpc().get().sendTraces(sendTracesRequest))
                        .methodDescriptor(GatewayGrpc.getSendTracesMethod())
                        .build());

                removeBatch(logEntryList);
            }
        }
    }

    public void log(long startTime, long endTime, MethodDescriptor<?, ?> methodInvoked, Status.Code returnCode, String traceParentHeader, String optionalErrorMsg) {
        LOG.debug("received cloud log with startTime={}, endTime={}, methodInvoked={}, returnCode={}, traceParentHeader={}",
                startTime, endTime, methodInvoked.getFullMethodName(), returnCode, traceParentHeader);

        if (!contains(methodInvoked.getFullMethodName(), SEND_TRACES_METHOD)) {
            logEntryQueue.add(LogEntry.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .methodInvoked(methodInvoked)
                    .returnCode(returnCode)
                    .optionalErrorMsg(optionalErrorMsg)
                    .traceParentHeader(traceParentHeader)
                    .build());
        }
    }

    public int getLogEntryQueueSize() {
        return logEntryQueue.size();
    }

    public List<LogEntry> getQueueBatch(int batchSize) {
        return logEntryQueue.stream()
                .limit(batchSize).collect(Collectors.toList());
    }

    public void removeBatch(List<LogEntry> elementsToBeRemoved) {
        logEntryQueue.removeAll(elementsToBeRemoved);
        LOG.debug("Removed from log entry queue {} elements; current size: {}", elementsToBeRemoved.size(), getLogEntryQueueSize());
    }

    public boolean isQueueEmpty() {
        return logEntryQueue.isEmpty();
    }

    public boolean isQueueNotEmpty() {
        return !logEntryQueue.isEmpty();
    }

    public void deleteAll() {
        LOG.debug("Removing from log entry queue {} elements.", getLogEntryQueueSize());
        logEntryQueue.clear();
    }

    public void destroy() {
        CloseUtil.close(this.grpc);
    }
}