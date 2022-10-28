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

package org.opennms.plugins.cloud.testserver;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.opennms.plugins.cloud.testserver.FileUtil.classpathFileToString;

import java.time.Instant;
import java.util.Objects;

import org.opennms.dataplatform.access.AuthenticateGrpc;
import org.opennms.dataplatform.access.AuthenticateOuterClass;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.RegistrationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;


public class ConfigGrpcImpl extends AuthenticateGrpc.AuthenticateImplBase implements BindableService {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigGrpcImpl.class);


    private GrpcConnectionConfig config;

    public void init(final GrpcConnectionConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public void authenticateKey(AuthenticateOuterClass.AuthenticateKeyRequest request, StreamObserver<AuthenticateOuterClass.AuthenticateKeyResponse> responseObserver) {
        LOG.info("authenticateKey() called.");
        if (this.config == null) {
            LOG.warn("call init() first.");
            responseObserver.onError(new IllegalAccessException("call init() first"));
            return;
        }
        AuthenticateOuterClass.AuthenticateKeyResponse response = AuthenticateOuterClass.AuthenticateKeyResponse.newBuilder()
                .setCertificate(classpathFileToString("/cert/client_cert.crt"))
                .setPrivateKey(classpathFileToString("/cert/client_private_key.key"))
                .setGrpcEndpoint(config.getHost() + ":" + config.getPort())
                .build();
        responseObserver.onNext(response);
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
        Algorithm algorithm = Algorithm.HMAC256("secret");
        String token = JWT.create()
                .withExpiresAt(Instant.now().plus(1, HOURS))
                .sign(algorithm);
        responseObserver.onNext(AuthenticateOuterClass.GetAccessTokenResponse.newBuilder()
                .setToken(token)
                .build());
        responseObserver.onCompleted();
    }

    public void renewCertificate(AuthenticateOuterClass.RenewCertificateRequest request, StreamObserver<AuthenticateOuterClass.RenewCertificateResponse> responseObserver) {
        LOG.info("renewCertificate() called.");

        AuthenticateOuterClass.RenewCertificateResponse response = AuthenticateOuterClass.RenewCertificateResponse.newBuilder()
                .setCertificate(classpathFileToString("/cert/client_cert.crt"))
                .setPrivateKey(classpathFileToString("/cert/client_private_key.key"))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
