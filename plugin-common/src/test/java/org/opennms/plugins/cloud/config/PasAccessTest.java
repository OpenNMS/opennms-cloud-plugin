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
import static org.opennms.plugins.cloud.testserver.FileUtil.classpathFileToString;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.opennms.dataplatform.access.AuthenticateGrpc;
import org.opennms.dataplatform.access.AuthenticateOuterClass;
import org.opennms.plugins.cloud.config.ConfigStore.Key;
import org.opennms.plugins.cloud.grpc.GrpcConnection;
import org.opennms.plugins.cloud.srv.RegistrationManager;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;

public class PasAccessTest {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Test
    public void shouldConfigure() throws IOException {
        final String serverHost = "myHost";
        final int serverPort = 12345;
        final String privateKey = classpathFileToString("/cert/client_private_key.key");
        final String certificate = classpathFileToString("/cert/client_cert.crt");

        AuthenticateGrpc.AuthenticateImplBase authService =
                new AuthenticateGrpc.AuthenticateImplBase() {
                    @Override
                    public void authenticateKey(AuthenticateOuterClass.AuthenticateKeyRequest request, StreamObserver<AuthenticateOuterClass.AuthenticateKeyResponse> responseObserver) {
                        responseObserver.onNext(AuthenticateOuterClass.AuthenticateKeyResponse.newBuilder()
                                .setGrpcEndpoint(serverHost + ":" + serverPort)
                                .setPrivateKey(privateKey)
                                .setCertificate(certificate)
                                .build());
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void getServices(AuthenticateOuterClass.GetServicesRequest request, StreamObserver<AuthenticateOuterClass.GetServicesResponse> responseObserver) {
                        responseObserver.onNext(AuthenticateOuterClass.GetServicesResponse.newBuilder()
                                .putServices(RegistrationManager.Service.TSAAS.name(), AuthenticateOuterClass.Service.newBuilder()
                                        .setEnabled(true)
                                        .build())
                                .putServices(RegistrationManager.Service.FAAS.name(), AuthenticateOuterClass.Service.newBuilder()
                                        .setEnabled(false)
                                        .build())
                                .build());
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void getAccessToken(AuthenticateOuterClass.GetAccessTokenRequest request, StreamObserver<AuthenticateOuterClass.GetAccessTokenResponse> responseObserver) {
                        responseObserver.onNext(AuthenticateOuterClass.GetAccessTokenResponse.newBuilder()
                                        .setToken("myToken")
                                        .build());
                        responseObserver.onCompleted();
                    }

                    @Override
                    public void renewCertificate(AuthenticateOuterClass.RenewCertificateRequest request, StreamObserver<AuthenticateOuterClass.RenewCertificateResponse> responseObserver) {
                        responseObserver.onNext(AuthenticateOuterClass.RenewCertificateResponse.newBuilder()
                                .setPrivateKey(privateKey)
                                .setCertificate(certificate)
                                .build());
                        responseObserver.onCompleted();
                    }
                };

        final Server server = grpcCleanup.register(
                InProcessServerBuilder.forName(PasAccessTest.class.getSimpleName())
                        .directExecutor()
                        .addService(authService)
                        .build()
                        .start());
        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(PasAccessTest.class.getSimpleName())
                        .directExecutor()
                        .build());

        AuthenticateGrpc.AuthenticateBlockingStub stub = AuthenticateGrpc.newBlockingStub(channel);
        GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> grpc = new GrpcConnection<>(stub, channel);
        PasAccess pas = new PasAccess(grpc);

        Map<Key, String> config = pas.getCredentialsFromAccessService("key", "systemId");
        assertEquals(serverHost, config.get(ConfigStore.Key.grpchost));
        assertEquals(Integer.toString(serverPort), config.get(Key.grpcport));
        assertEquals(privateKey, config.get(ConfigStore.Key.privatekey));
        assertEquals(certificate, config.get(Key.publickey));
        // FAAS shouldn't show up since it is not enabled:
        assertEquals(Set.of(RegistrationManager.Service.TSAAS), pas.getActiveServices("systemId"));
        assertEquals("myToken", pas.getToken(new HashSet<>(), "systemId"));

        // renew certs
        Map<Key, String> newCerts = pas.renewCertificate("systemId");
        assertEquals(privateKey, newCerts.get(Key.privatekey));
        assertEquals(certificate, newCerts.get(Key.publickey));

        server.shutdown();
    }

}