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

package org.opennms.plugins.cloud.config;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.opennms.plugins.cloud.config.SecureCredentialsVaultUtil.Type;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.opennms.dataplatform.access.AuthenticateGrpc;
import org.opennms.dataplatform.access.AuthenticateOuterClass;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.RegistrationManager;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

public class ConfigurationManagerTest {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Test
    public void shouldConfigure() throws IOException {
        ConfigZipExtractor zip =  new ConfigZipExtractor(Path.of("src/test/resources/cert/cloud-credentials.zip"));
        InMemoryScv scv = new InMemoryScv();
        final String endpoint = "endpoint";
        final String privateKey = zip.getPrivateKey();
        final String certificate = zip.getPublicKey();

        AuthenticateGrpc.AuthenticateImplBase authService =
                new AuthenticateGrpc.AuthenticateImplBase() {
                    @Override
                    public void authenticateKey(AuthenticateOuterClass.AuthenticateKeyRequest request, StreamObserver<AuthenticateOuterClass.AuthenticateKeyResponse> responseObserver) {
                        responseObserver.onNext(AuthenticateOuterClass.AuthenticateKeyResponse.newBuilder()
                                .setGrpcEndpoint(endpoint)
                                .setPrivateKey(privateKey)
                                .setCertificate(certificate)
                                .build());
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void getServices(AuthenticateOuterClass.GetServicesRequest request, StreamObserver<AuthenticateOuterClass.GetServicesResponse> responseObserver) {
                        responseObserver.onNext(AuthenticateOuterClass.GetServicesResponse.newBuilder()
                                .putServices("tsaas", AuthenticateOuterClass.Service.newBuilder()
                                        .setEnabled(true)
                                        .build())
                                .putServices("faas", AuthenticateOuterClass.Service.newBuilder()
                                        .setEnabled(false)
                                        .build())
                                .build());
                        responseObserver.onCompleted();
                    }
                };

        Server server = grpcCleanup.register(
                InProcessServerBuilder.forName(ConfigurationManagerTest.class.getSimpleName())
                        .directExecutor()
                        .addService(authService)
                        .build()
                        .start());
        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(ConfigurationManagerTest.class.getSimpleName())
                        .directExecutor()
                        .build());
        AuthenticateGrpc.AuthenticateBlockingStub grpc = AuthenticateGrpc.newBlockingStub(channel);


        ConfigurationManager cm = new ConfigurationManager(
                scv,
                GrpcConnectionConfig.builder()
                        .clientTrustStore(Files.readString(Path.of("src/test/resources/cert/clienttruststore.pem")))
                        .build(),
                GrpcConnectionConfig.builder()
                        .clientTrustStore(Files.readString(Path.of("src/test/resources/cert/clienttruststore.pem")))
                        .build(),
                mock(RegistrationManager.class),
                new ArrayList<>(),
                grpc);
        assertEquals(ConfigurationManager.ConfigStatus.NOT_ATTEMPTED, cm.getStatus());
        GrpcConnectionConfig grpcConnectionConfig = cm.fetchCredentialsFromAccessService("something");
        cm.storeCredentials(grpcConnectionConfig);
        SecureCredentialsVaultUtil scvUtil = new SecureCredentialsVaultUtil(scv);
        assertEquals(certificate, scvUtil.getOrNull(Type.publickey));
        assertEquals(privateKey, scvUtil.getOrNull(Type.privatekey));

        // TODO: check for token assertEquals(certificate, scvUtil.getOrNull(Type.token));
        server.shutdown();
    }

}