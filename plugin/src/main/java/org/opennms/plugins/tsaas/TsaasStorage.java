/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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

package org.opennms.plugins.tsaas;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import org.opennms.integration.api.v1.timeseries.Aggregation;
import org.opennms.integration.api.v1.timeseries.Metric;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.integration.api.v1.timeseries.Tag;
import org.opennms.integration.api.v1.timeseries.TagMatcher;
import org.opennms.integration.api.v1.timeseries.TimeSeriesFetchRequest;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.tsaas.TimeseriesGrpc;
import org.opennms.tsaas.Tsaas;
import org.opennms.tsaas.grpc.GrpcObjectMapper;
import org.opennms.tsaas.grpc.comp.ZStdCodecRegisterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Timestamp;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * The OpenNMS Time series as-a-service storage plugin implementation.
 * <p>
 * This implementation forwards time series requests to the OpenNMS time series service running in the cloud.
 */
public class TsaasStorage implements TimeSeriesStorage {
    private static final Logger LOG = LoggerFactory.getLogger(TsaasStorage.class);
    private static final String TOKEN_KEY = "token";
    private final TimeseriesGrpc.TimeseriesBlockingStub clientStub;
    private final String clientToken;

    private final ManagedChannel managedChannel;

    private class TokenAddingInterceptor implements ClientInterceptor {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                                   CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                @Override
                public void start(final Listener<RespT> responseListener, final Metadata headers) {
                    headers.put(Metadata.Key.of(TOKEN_KEY, Metadata.ASCII_STRING_MARSHALLER), clientToken);
                    super.start(responseListener, headers);
                }
            };
        }
    }

    public TsaasStorage(String host, int port, String clientToken) {
        this.clientToken = clientToken;
        LOG.debug("Starting with host {} and port {}", host, port);
        try {
            final NettyChannelBuilder builder = NettyChannelBuilder.forAddress(host, port);
            // FIXME: HACKZ: Need more elegant way of enabling/disabling SSL
            if (port == 443) {
                builder.useTransportSecurity()
                        // FIXE: HACKZ: Disabling cert checks should be optional
                        .sslContext(GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build());
            } else {
                builder.usePlaintext();
            }
            managedChannel = builder
                    .compressorRegistry(ZStdCodecRegisterUtil.createCompressorRegistry())
                    .decompressorRegistry(ZStdCodecRegisterUtil.createDecompressorRegistry())
                    .build();
            clientStub = TimeseriesGrpc.newBlockingStub(managedChannel)
                    .withCompression("gzip") // ZStdGrpcCodec.ZSTD
                    .withInterceptors(new TokenAddingInterceptor());
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Tsaas.Tag toTag(Tag tag) {
        return Tsaas.Tag.newBuilder()
                .setKey(tag.getKey())
                .setValue(tag.getValue())
                .build();
    }

    private static Tsaas.Metric toMetric(Metric metric) {
        return Tsaas.Metric.newBuilder()
                .setKey(metric.getKey())
                .addAllIntrinsicTags(metric.getIntrinsicTags().stream()
                        .map(TsaasStorage::toTag).collect(Collectors.toSet()))
                .addAllMetaTags(metric.getMetaTags().stream()
                        .map(TsaasStorage::toTag).collect(Collectors.toSet()))
                .addAllExternalTags(metric.getExternalTags().stream()
                        .map(TsaasStorage::toTag).collect(Collectors.toSet()))
                .build();
    }

    private static Tsaas.Sample toSample(Sample sample) {
        return Tsaas.Sample.newBuilder()
                .setMetric(toMetric(sample.getMetric()))
                .setTime(toTimestamp(sample.getTime()))
                .setValue(sample.getValue())
                .build();
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                // TODO: Is this the correct way to craft a proto timestamp from an instant?
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    @Override
    public void store(List<Sample> samples) throws StorageException {
        // TODO: The client is responsible for ensuring the message is fully-formed
        List<Tsaas.Sample> mappedSamples = samples.stream()
                .map(TsaasStorage::toSample)
                .collect(Collectors.toList());
        Tsaas.Samples samplesMessage = Tsaas.Samples.newBuilder()
                .addAllSamples(mappedSamples)
                .build();
        LOG.trace("Storing the following samples: {}", samplesMessage);
        clientStub.store(samplesMessage);
    }

    @Override
    public List<Metric> findMetrics(Collection<TagMatcher> tagMatchers) throws StorageException {
        Objects.requireNonNull(tagMatchers);
        if(tagMatchers.isEmpty()) {
            throw new IllegalArgumentException("at least one TagMatcher is required.");
        }

        // TODO: The client is responsible for ensuring the message is fully-formed
        List<Tsaas.TagMatcher> mappedTags = tagMatchers.stream()
                .map(GrpcObjectMapper::toTagMatcher)
                .collect(Collectors.toList());
        Tsaas.TagMatchers tagsMessage = Tsaas.TagMatchers.newBuilder()
                .addAllMatchers(mappedTags)
                .build();
        LOG.trace("Getting the metrics for the following tags: {}", tagsMessage);
        Tsaas.Metrics result = clientStub.findMetrics(tagsMessage);
        return GrpcObjectMapper.toMetrics(result);
    }

    @Override
    public List<Sample> getTimeseries(TimeSeriesFetchRequest request) throws StorageException {
        // TODO: The client is responsible for ensuring the message is fully-formed
        Objects.requireNonNull(request.getMetric());
        Tsaas.FetchRequest fetchRequest = Tsaas.FetchRequest.newBuilder()
                .setMetric(toMetric(request.getMetric()))
                .setStart(toTimestamp(request.getStart()))
                .setEnd(toTimestamp(request.getEnd()))
                .setStep(request.getStep().toMillis())
                .setAggregation(Tsaas.Aggregation.valueOf(request.getAggregation().name()))
                .build();
        LOG.trace("Getting timeseries for request: {}", fetchRequest);
        Tsaas.Samples samples = clientStub.getTimeseries(fetchRequest);
        return GrpcObjectMapper.toSamples(samples);
    }

    @Override
    public void delete(Metric metric) throws StorageException {
        LOG.warn("Attempted to delete metric {} but deleting is unsupported", metric);
        throw new UnsupportedOperationException("Deleting is not currently supported");
    }

    @Override
    public boolean supportsAggregation(Aggregation aggregation) {
        // TODO: Add a client side impl here
        return false;
    }

    public void destroy() {
        if (managedChannel != null) {
            managedChannel.shutdownNow();
            try {
                managedChannel.awaitTermination(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.info("Interrupted while awaiting for channel to shutdown.");
            }
        }
    }
}
