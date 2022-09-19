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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.net.ssl.SSLException;

import org.opennms.plugins.cloud.srv.tsaas.grpc.comp.ZStdCodecRegisterUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

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
import io.grpc.netty.shaded.io.netty.handler.ssl.OpenSsl;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import io.grpc.stub.AbstractBlockingStub;

public class GrpcConnection<T extends AbstractBlockingStub<T>> implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcConnection.class);
    // 100M sync with cortex server
    private static final int MAX_MESSAGE_SIZE = 104857600;
    @VisibleForTesting
    public final ManagedChannel managedChannel;
    private final T clientStub;

    public GrpcConnection(final GrpcConnectionConfig config, final Function<ManagedChannel,T> stubCreator) {
        final NettyChannelBuilder builder = NettyChannelBuilder.forAddress(config.getHost(), config.getPort());
        if (GrpcConnectionConfig.Security.PLAIN_TEXT == config.getSecurity()) {
            builder.usePlaintext();
        } else {
            builder.sslContext(createSslContext(config));
        }
        // setup message size & keepalive time DC-282
        builder.maxInboundMessageSize(MAX_MESSAGE_SIZE).maxInboundMetadataSize(MAX_MESSAGE_SIZE)
                .keepAliveTime(10, TimeUnit.SECONDS)
                .keepAliveTimeout(5, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true);
        managedChannel = builder
                .compressorRegistry(ZStdCodecRegisterUtil.createCompressorRegistry())
                .decompressorRegistry(ZStdCodecRegisterUtil.createDecompressorRegistry())
                .build();
        clientStub = stubCreator.apply(managedChannel)
                .withCompression("gzip") // ZStdGrpcCodec.ZSTD
                .withInterceptors(new TokenAddingInterceptor(config));
    }

    public GrpcConnection(T clientStub, ManagedChannel managedChannel) {
        this.clientStub = Objects.requireNonNull(clientStub);
        this.managedChannel = Objects.requireNonNull(managedChannel);
    }

    public T get() {
        return this.clientStub;
    }

    private SslContext createSslContext(final GrpcConnectionConfig config) {
        Objects.requireNonNull(config);
        try {
            final SslProvider provider = OpenSsl.isAvailable() && SslProvider.isAlpnSupported(SslProvider.OPENSSL) ? SslProvider.OPENSSL : SslProvider.JDK;
            LOG.info("Using SSL provider {}, ", provider);
            SslContextBuilder context = GrpcSslContexts.configure(GrpcSslContexts.forClient(), provider);
            final String truststore = config.getClientTrustStore();
            if (truststore == null) {
                LOG.info("Will use jvm truststore.");
            } else {
                LOG.info("Will use truststore from SecureCredentialsVault.");
                context.trustManager(new ByteArrayInputStream(truststore.getBytes(StandardCharsets.UTF_8)));
            }
            if (GrpcConnectionConfig.Security.MTLS == config.getSecurity()) {
                context.keyManager(
                                new ByteArrayInputStream(config.getPublicKey().getBytes(StandardCharsets.UTF_8)),
                                new ByteArrayInputStream(config.getPrivateKey().getBytes(StandardCharsets.UTF_8)))
                        .clientAuth(ClientAuth.REQUIRE);
            }
            return context.build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws InterruptedException {
        if (managedChannel != null) {
            managedChannel.shutdownNow();
            managedChannel.awaitTermination(15, TimeUnit.SECONDS);
        }
    }

    private static class TokenAddingInterceptor implements ClientInterceptor {

        final String tokenKey;
        final String tokenValue;

        TokenAddingInterceptor(final GrpcConnectionConfig config) {
            this.tokenKey = config.getTokenKey();
            this.tokenValue = config.getTokenValue();
        }

        @Override
        public <I, O> ClientCall<I, O> interceptCall(MethodDescriptor<I, O> method,
                                                     CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                @Override
                public void start(final Listener<O> responseListener, final Metadata headers) {
                    if(tokenKey != null && !tokenKey.isBlank() && tokenValue != null && !tokenValue.isBlank()) {
                        headers.put(Metadata.Key.of(tokenKey, Metadata.ASCII_STRING_MARSHALLER), tokenValue);
                    }
                    super.start(responseListener, headers);
                }
            };
        }
    }
}
