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

import static org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.SCV_ALIAS;
import static org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.Type.privatekey;
import static org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.Type.publickey;

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
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.SSLException;

import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.timeseries.Aggregation;
import org.opennms.integration.api.v1.timeseries.Metric;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.integration.api.v1.timeseries.Tag;
import org.opennms.integration.api.v1.timeseries.TagMatcher;
import org.opennms.integration.api.v1.timeseries.TimeSeriesFetchRequest;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.Type;
import org.opennms.plugins.cloud.tsaas.grpc.GrpcObjectMapper;
import org.opennms.plugins.cloud.tsaas.grpc.comp.ZStdCodecRegisterUtil;
import org.opennms.plugins.cloud.tsaas.shell.CertificateImporter;
import org.opennms.tsaas.TimeseriesGrpc;
import org.opennms.tsaas.Tsaas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OpenNMS Time series as-a-service storage plugin implementation.
 * <p>
 * This implementation forwards time series requests to the OpenNMS time series service running in the cloud.
 */
public class TsaasStorage implements TimeSeriesStorage {
    private static final Logger LOG = LoggerFactory.getLogger(TsaasStorage.class);

    private final TimeseriesGrpc.TimeseriesBlockingStub clientStub;
    private final TsaasConfig config;
    private final SecureCredentialsVaultUtil scvUtil;
    private final ManagedChannel managedChannel;
    private final ConcurrentLinkedDeque<Tsaas.Sample> queue; // holds samples to be batched
    private Instant lastBatchSentTs;

    private class TokenAddingInterceptor implements ClientInterceptor {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                                   CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                @Override
                public void start(final Listener<RespT> responseListener, final Metadata headers) {
                    String token = scvUtil.getCredentials()
                            .map(c -> c.getAttribute(Type.token.name()))
                            .orElse("--not defined--");
                    headers.put(Metadata.Key.of(config.getTokenKey(), Metadata.ASCII_STRING_MARSHALLER), token);
                    super.start(responseListener, headers);
                }
            };
        }
    }

    public TsaasStorage(TsaasConfig config, SecureCredentialsVault scv) {
        this.config = Objects.requireNonNull(config);
        this.scvUtil = new SecureCredentialsVaultUtil(scv);
        importCloudCredentialsIfPresent(scv);
        LOG.debug("Starting with host {} and port {}", config.getHost(), config.getPort());

        final NettyChannelBuilder builder = NettyChannelBuilder.forAddress(config.getHost(), config.getPort());
        if (config.isMtlsEnabled()) {
            builder.sslContext(createSslContext());
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
        queue = new ConcurrentLinkedDeque<>();
        lastBatchSentTs = Instant.now();
    }

    void importCloudCredentialsIfPresent(final SecureCredentialsVault scv) {

        Path cloudCredentialsFile = Path.of(System.getProperty("opennms.home") + "/etc/cloud-credentials.zip");
        if (Files.exists(cloudCredentialsFile)) {
            try {
                CertificateImporter importer = new CertificateImporter(cloudCredentialsFile.toString(), scv,
                        (s) -> LoggerFactory.getLogger(CertificateImporter.class).info(s));
                importer.execute();
            } catch (Exception e) {
                LOG.warn("Could not import {}. Will continue with old credentials.", cloudCredentialsFile, e);
            }
        }
    }

    private SslContext createSslContext() {
        Objects.requireNonNull(scvUtil);
        Credentials credentials = scvUtil.getCredentials()
                .orElseThrow(() -> new NullPointerException(
                        String.format("Could no find credentials in SecureCredentialsVault for %s. Please import via Karaf shell: opennms-tsaas:import-cert", SCV_ALIAS)));

        try {
            SslContextBuilder context = GrpcSslContexts.configure(GrpcSslContexts.forClient(), SslProvider.OPENSSL);
            String truststore = credentials.getAttribute(Type.truststore.name());
            if (truststore == null) {
                LOG.info("Will use jvm truststore.");
            } else {
                LOG.info("Will use truststore from SecureCredentialsVault.");
                context.trustManager(new ByteArrayInputStream(truststore.getBytes(StandardCharsets.UTF_8)));
            }

            context.keyManager(
                            getStreamFromAttribute(credentials, publickey),
                            getStreamFromAttribute(credentials, privatekey))
                    .clientAuth(ClientAuth.REQUIRE);
            return context.build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    private ByteArrayInputStream getStreamFromAttribute(Credentials credentials, Type key) {
        String attribute = Objects.requireNonNull(credentials.getAttribute(key.name()),
                String.format("Could no find attribute %s in SecureCredentialsVault for %s", key, SCV_ALIAS));
        return new ByteArrayInputStream(attribute.getBytes(StandardCharsets.UTF_8));
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
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    @Override
    public void store(List<Sample> samples) throws StorageException {

        // convert given samples to grpc
        samples.stream()
                .map(TsaasStorage::toSample)
                .forEach(this.queue::add);

        // send batched messages while the queue is fuller than batch size
        while (this.queue.size() >= this.config.getBatchSize() ||
                this.queue.size() > 0 && this.lastBatchSentTs.plusMillis(config.getMaxBatchWaitTimeInMilliSeconds()).isBefore(Instant.now())) {
            Tsaas.Samples.Builder builder = Tsaas.Samples.newBuilder();
            for (int i = 0; i < this.config.getBatchSize(); i++) {
                Tsaas.Sample next = this.queue.poll();
                if (next != null) {
                    builder.addSamples(next);
                } else {
                    break; // queue is empty => nothing more to do. This can happen since we are in a multithreaded environment.
                }
            }
            // Make call (only if we have anything to send):
            if (builder.getSamplesCount() > 0) {
                clientStub.store(builder.build());
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
        Tsaas.Metrics result = clientStub.findMetrics(tagsMessage);
        return GrpcObjectMapper.toMetrics(result);
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
