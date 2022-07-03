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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.plugins.cloud.tsaas.grpc.comp.ZStdCodecRegisterUtil;
import org.opennms.tsaas.TimeseriesGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class GrpcConnection {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcConnection.class);
    // 100M sync with cortex server
    private final static int MAX_MESSAGE_SIZE = 104857600;

    private ManagedChannel managedChannel;
    private TimeseriesGrpc.TimeseriesBlockingStub clientStub;

    public void init(final TsaasConfig config, final SecureCredentialsVaultUtil scvUtil) {
        shutDown();
        final NettyChannelBuilder builder = NettyChannelBuilder.forAddress(config.getHost(), config.getPort());
        if (config.isMtlsEnabled()) {
            builder.sslContext(createSslContext(scvUtil));
        } else {
            builder.usePlaintext();
        }
        // setup message size
        builder.maxInboundMessageSize(MAX_MESSAGE_SIZE).maxInboundMetadataSize(MAX_MESSAGE_SIZE);
        managedChannel = builder
                .compressorRegistry(ZStdCodecRegisterUtil.createCompressorRegistry())
                .decompressorRegistry(ZStdCodecRegisterUtil.createDecompressorRegistry())
                .build();
        clientStub = TimeseriesGrpc.newBlockingStub(managedChannel)
                .withCompression("gzip") // ZStdGrpcCodec.ZSTD
                .withInterceptors(new TokenAddingInterceptor(config, scvUtil));
    }

    public TimeseriesGrpc.TimeseriesBlockingStub get() {
        return this.clientStub;
    }

    private SslContext createSslContext(final SecureCredentialsVaultUtil scvUtil) {
        Objects.requireNonNull(scvUtil);
        Credentials credentials = scvUtil.getCredentials()
                .orElseThrow(() -> new NullPointerException(
                        String.format("Could no find credentials in SecureCredentialsVault for %s. Please import via Karaf shell: opennms-cloud:import-cert", SCV_ALIAS)));

        try {
            SslContextBuilder context = GrpcSslContexts.configure(GrpcSslContexts.forClient(), SslProvider.OPENSSL);
            String truststore = credentials.getAttribute(SecureCredentialsVaultUtil.Type.truststore.name());
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

    private ByteArrayInputStream getStreamFromAttribute(Credentials credentials, SecureCredentialsVaultUtil.Type key) {
        String attribute = Objects.requireNonNull(credentials.getAttribute(key.name()),
                String.format("Could no find attribute %s in SecureCredentialsVault for %s", key, SCV_ALIAS));
        return new ByteArrayInputStream(attribute.getBytes(StandardCharsets.UTF_8));
    }

    public void shutDown() {
        if (managedChannel != null) {
            managedChannel.shutdownNow();
            try {
                managedChannel.awaitTermination(15, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOG.info("Interrupted while awaiting for channel to shutdown.");
            }
        }
    }

    private static class TokenAddingInterceptor implements ClientInterceptor {

        final String tokenKey;
        final String token;

        TokenAddingInterceptor(final TsaasConfig config, final SecureCredentialsVaultUtil scvUtil) {
            this.tokenKey = config.getTokenKey();
            String token = scvUtil.getCredentials()
                    .map(c -> c.getAttribute(SecureCredentialsVaultUtil.Type.token.name()))
                    .orElse(config.getTokenValue()); // fallback
            if (token == null || token.isEmpty()) {
                token = "--not defined--";
            }
            this.token = token;
        }

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                                   CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
                @Override
                public void start(final Listener<RespT> responseListener, final Metadata headers) {
                    headers.put(Metadata.Key.of(tokenKey, Metadata.ASCII_STRING_MARSHALLER), token);
                    super.start(responseListener, headers);
                }
            };
        }
    }
}
