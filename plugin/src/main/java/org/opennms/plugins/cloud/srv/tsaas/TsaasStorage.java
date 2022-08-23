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

import static org.opennms.plugins.cloud.grpc.GrpcExceptionHandler.executeRpcCall;
import static org.opennms.plugins.cloud.srv.tsaas.grpc.GrpcObjectMapper.toMetric;
import static org.opennms.plugins.cloud.srv.tsaas.grpc.GrpcObjectMapper.toTimestamp;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.opennms.integration.api.v1.timeseries.Aggregation;
import org.opennms.integration.api.v1.timeseries.Metric;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.integration.api.v1.timeseries.TagMatcher;
import org.opennms.integration.api.v1.timeseries.TimeSeriesFetchRequest;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.grpc.GrpcConnection;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.plugins.cloud.srv.tsaas.grpc.GrpcObjectMapper;
import org.opennms.tsaas.TimeseriesGrpc;
import org.opennms.tsaas.Tsaas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

/**
 * The OpenNMS Time series as-a-service storage plugin implementation.
 * <p>
 * This implementation forwards time series requests to the OpenNMS time series service running in the cloud.
 */
public class TsaasStorage implements TimeSeriesStorage, GrpcService {
    private static final Logger LOG = LoggerFactory.getLogger(TsaasStorage.class);
    private final TsaasConfig config;
    private final ConcurrentLinkedDeque<Tsaas.Sample> queue; // holds samples to be batched
    private Instant lastBatchSentTs;
    @VisibleForTesting
    GrpcConnection<TimeseriesGrpc.TimeseriesBlockingStub> grpc;

    public TsaasStorage(TsaasConfig config) {
        this.config = Objects.requireNonNull(config);
        queue = new ConcurrentLinkedDeque<>();
        lastBatchSentTs = Instant.now();
    }

    @Override
    public void initGrpc(GrpcConnectionConfig grpcConfig) {
        GrpcConnection<TimeseriesGrpc.TimeseriesBlockingStub> oldGrpc = this.grpc;
        LOG.debug("Initializing Grpc Connection with host {} and port {}", grpcConfig.getHost(), grpcConfig.getPort());
        this.grpc = new GrpcConnection<>(grpcConfig, TimeseriesGrpc::newBlockingStub);
        if(oldGrpc != null) {
            try {
                oldGrpc.shutDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void store(List<Sample> samples) throws StorageException {

        // convert given samples to grpc
        samples.stream()
                .map(GrpcObjectMapper::toSample)
                .forEach(this.queue::add);

        // send batched messages while the queue is fuller than batch size
        while (this.queue.size() >= this.config.getBatchSize() ||
                !this.queue.isEmpty() && this.lastBatchSentTs.plusMillis(config.getMaxBatchWaitTimeInMilliSeconds()).isBefore(Instant.now())) {
            Tsaas.Samples.Builder builder = Tsaas.Samples.newBuilder();
            for (int i = 0; i < this.config.getBatchSize(); i++) {
                Tsaas.Sample next = this.queue.poll();
                if (next != null) {
                    builder.addSamples(next);
                } else {
                    break; // queue is empty => nothing more to do. This can happen since we are in a multi threaded environment.
                }
            }
            // Make call (only if we have anything to send):
            if (builder.getSamplesCount() > 0) {
                executeRpcCall(() -> this.grpc.get().store(builder.build()));
                lastBatchSentTs = Instant.now();
            }
        }
    }

    @Override
    public List<Metric> findMetrics(Collection<TagMatcher> tagMatchers) throws StorageException {
        Objects.requireNonNull(tagMatchers);
        if (tagMatchers.isEmpty()) {
            throw new IllegalArgumentException("at least one TagMatcher is required.");
        }

        List<Tsaas.TagMatcher> mappedTags = tagMatchers.stream()
                .map(GrpcObjectMapper::toTagMatcher)
                .collect(Collectors.toList());
        Tsaas.TagMatchers tagsMessage = Tsaas.TagMatchers.newBuilder()
                .addAllMatchers(mappedTags)
                .build();
        LOG.trace("Getting the metrics for the following tags: {}", tagsMessage);
        return executeRpcCall(
            () -> this.grpc.get().findMetrics(tagsMessage),
            GrpcObjectMapper::toMetrics,
            Collections::emptyList
        );
    }

    @Override
    public List<Sample> getTimeseries(TimeSeriesFetchRequest request) throws StorageException {
        Objects.requireNonNull(request.getMetric());
        Tsaas.FetchRequest fetchRequest = Tsaas.FetchRequest.newBuilder()
                .setMetric(toMetric(request.getMetric()))
                .setStart(toTimestamp(request.getStart()))
                .setEnd(toTimestamp(request.getEnd()))
                .setStep(request.getStep().getSeconds())
                .setAggregation(Tsaas.Aggregation.valueOf(request.getAggregation().name()))
                .build();
        LOG.trace("Getting time series for request: {}", fetchRequest);
        return executeRpcCall(
            () -> this.grpc.get().getTimeseriesData(fetchRequest),
            GrpcObjectMapper::toSamples,
            Collections::emptyList
        );
    }

    @Override
    public void delete(Metric metric) throws StorageException {
        LOG.warn("Attempted to delete metric {} but deleting is unsupported", metric);
        throw new UnsupportedOperationException("Deleting is not currently supported");
    }

    @Override
    public boolean supportsAggregation(Aggregation aggregation) {
        return false;
    }

    public Tsaas.CheckHealthResponse checkHealth() {
        return this.grpc.get().checkHealth(Tsaas.CheckHealthRequest.newBuilder().build());
    }

    public void destroy() throws InterruptedException {
        grpc.shutDown();
    }
}
