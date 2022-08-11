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

package org.opennms.plugins.cloud.srv.tsaas.testserver;

import static org.opennms.plugins.cloud.srv.tsaas.grpc.GrpcObjectMapper.toMetric;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.opennms.integration.api.v1.timeseries.Metric;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.integration.api.v1.timeseries.TagMatcher;
import org.opennms.integration.api.v1.timeseries.TimeSeriesData;
import org.opennms.integration.api.v1.timeseries.TimeSeriesFetchRequest;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.srv.tsaas.grpc.GrpcObjectMapper;
import org.opennms.tsaas.TimeseriesGrpc;
import org.opennms.tsaas.Tsaas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;

/**
 * Provides endpoint for grpc time series calls and translates and forwards them to the given TimeseriesStorage implementation.
 */
public class TimeseriesGrpcImpl extends TimeseriesGrpc.TimeseriesImplBase implements BindableService {

    private static final Logger LOG = LoggerFactory.getLogger(TimeseriesGrpcImpl.class);

    private final TimeSeriesStorage storage;

    public TimeseriesGrpcImpl(final TimeSeriesStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    @Override
    public void store(org.opennms.tsaas.Tsaas.Samples request,
                      io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        String clientID = TsassServerInterceptor.CLIENT_ID.get();
        LOG.debug("Store endpoint received {} samples with clientID {}", request.getSamplesCount(), clientID);
        List<Sample> samples = request
                .getSamplesList()
                .stream()
                .map(GrpcObjectMapper::toSample)
                .collect(Collectors.toList());
        try {
            storage.store(samples);
            responseObserver.onNext(com.google.protobuf.Empty.getDefaultInstance());
            responseObserver.onCompleted();
            LOG.debug("Successfully wrote {} samples.", request.getSamplesCount());
        } catch (StorageException e) {
            LOG.error("Failed to write {} samples.", request.getSamplesCount(), e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void findMetrics(org.opennms.tsaas.Tsaas.TagMatchers request,
                           io.grpc.stub.StreamObserver<Tsaas.Metrics> responseObserver) {
        String clientID = TsassServerInterceptor.CLIENT_ID.get();
        LOG.debug("findMetrics called with client ID {}", clientID);
        Collection<TagMatcher> tagMatchers = request.getMatchersList().stream().map(GrpcObjectMapper::toTagMatcher).collect(Collectors.toList());
        try {
            List<Metric> metrics;
            metrics = storage.findMetrics(tagMatchers);
            Tsaas.Metrics grpcMetrics = GrpcObjectMapper.toMetrics(metrics);
            responseObserver.onNext(grpcMetrics);
            responseObserver.onCompleted();
            LOG.debug("Found {} metrics.", grpcMetrics.getMetricsCount());
        } catch (StorageException e) {
            LOG.error("Failed to query metrics.", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getTimeseriesData(Tsaas.FetchRequest request,
                              io.grpc.stub.StreamObserver<Tsaas.TimeseriesData> responseObserver) {
        String clientID = TsassServerInterceptor.CLIENT_ID.get();
        LOG.debug("getTimeseries called with client ID {}", clientID);
        TimeSeriesFetchRequest fetchRequest = GrpcObjectMapper.toTimeseriesFetchRequest(request);
        try {
            TimeSeriesData timeSeriesData = storage.getTimeSeriesData(fetchRequest);
            List<Tsaas.DataPoint> dataPoints = timeSeriesData.getDataPoints().stream()
                    .map(GrpcObjectMapper::toDataPoint)
                    .collect(Collectors.toList());
            Tsaas.TimeseriesData timeseriesData = Tsaas.TimeseriesData.newBuilder()
                    .addAllDataPoints(dataPoints)
                    .setMetric(toMetric(timeSeriesData.getMetric()))
                    .build();
            responseObserver.onNext(timeseriesData);
            responseObserver.onCompleted();
            LOG.debug("Successfully queried timeseries - found {} samples.", timeSeriesData.getDataPoints().size());
        } catch (StorageException e) {
            LOG.error("Failed to retrieve timeseries.", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void checkHealth(Tsaas.CheckHealthRequest request, StreamObserver<Tsaas.CheckHealthResponse> responseObserver) {
        Tsaas.CheckHealthResponse response = Tsaas.CheckHealthResponse.newBuilder()
                .setStatus(Tsaas.CheckHealthResponse.ServingStatus.SERVING)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public void shutdown() {
    }

}
