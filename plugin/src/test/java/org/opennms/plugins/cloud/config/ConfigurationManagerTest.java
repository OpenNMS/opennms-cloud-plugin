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
import static org.opennms.plugins.cloud.srv.tsaas.SecureCredentialsVaultUtil.Type;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Rule;
import org.junit.Test;
import org.opennms.dataplatform.access.AuthenticateGrpc;
import org.opennms.dataplatform.access.AuthenticateOuterClass;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.ServiceManager;
import org.opennms.plugins.cloud.srv.tsaas.SecureCredentialsVaultUtil;

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
        InMemoryScv scv = new InMemoryScv();
        final String endpoint = "endpoint";
        final String privateKey = "privatekey";
        final String certificate = "certificate";

        AuthenticateGrpc.AuthenticateImplBase service =
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
                        .addService(service)
                        .build()
                        .start());
        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(ConfigurationManagerTest.class.getSimpleName())
                        .directExecutor()
                        .build());
        AuthenticateGrpc.AuthenticateBlockingStub grpc = AuthenticateGrpc.newBlockingStub(channel);


        ConfigurationManager cm = new ConfigurationManager(
                scv,
                GrpcConnectionConfig.builder().build(),
                mock(ServiceManager.class),
                new ArrayList<>(),
                grpc);
        assertEquals(ConfigurationManager.ConfigStatus.NOT_ATTEMPTED, cm.getStatus());
        cm.configure("something");
        assertEquals(ConfigurationManager.ConfigStatus.SUCCESSFUL, cm.getStatus());
        SecureCredentialsVaultUtil scvUtil = new SecureCredentialsVaultUtil(scv);
        assertEquals(certificate, scvUtil.getOrNull(Type.publickey));
        assertEquals(privateKey, scvUtil.getOrNull(Type.privatekey));

        // TODO: check for token assertEquals(certificate, scvUtil.getOrNull(Type.token));
        server.shutdown();
    }

}