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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.opennms.dataplatform.access.AuthenticateGrpc;
import org.opennms.dataplatform.access.AuthenticateOuterClass;
import org.opennms.plugins.cloud.grpc.GrpcConnection;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.RegistrationManager;

/** Handles calls to PAS (Platform Access Service). */
class PasAccess {

    final GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> grpc;
    PasAccess(final GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> grpc) {
        this.grpc = Objects.requireNonNull(grpc);
    }

    GrpcConnectionConfig fetchCredentialsFromAccessService(final String key, final String systemId) {

        AuthenticateOuterClass.AuthenticateKeyRequest keyRequest = AuthenticateOuterClass.AuthenticateKeyRequest.newBuilder()
                .setAuthenticationKey(key)
                .setSystemUuid(systemId)
                .build();
        AuthenticateOuterClass.AuthenticateKeyResponse response = grpc.get().authenticateKey(keyRequest);

        final GrpcConnectionConfig.GrpcConnectionConfigBuilder cloudGatewayConfig = GrpcConnectionConfig.builder();

        Optional
                .of(response.getGrpcEndpoint())
                .filter(s -> s.contains(":"))
                .map(s -> s.split(":"))
                .map(s -> s[0])
                .filter(s -> !s.isBlank())
                .ifPresent(cloudGatewayConfig::host);
        Optional
                .of(response.getGrpcEndpoint())
                .filter(s -> s.contains(":"))
                .map(s -> s.split(":"))
                .map(s -> s[1])
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .ifPresent(cloudGatewayConfig::port);
        return cloudGatewayConfig
                .publicKey(response.getCertificate())
                .privateKey(response.getPrivateKey())
                .security(GrpcConnectionConfig.Security.MTLS) // we always enable mtls (just not in tests)
                .build();
    }

    Set<RegistrationManager.Service> getActiveServices(final String systemId) {
        AuthenticateOuterClass.GetServicesResponse servicesResponse = grpc
                .get()
                .getServices(
                        AuthenticateOuterClass.GetServicesRequest.newBuilder()
                                .setSystemId(systemId)
                                .build());
        return servicesResponse
                .getServicesMap()
                .entrySet()
                .stream()
                .filter(e -> e.getValue().getEnabled())
                .map(Map.Entry::getKey)
                .map(RegistrationManager.Service::valueOf)
                .collect(Collectors.toSet());
    }

    String getToken(final Set<RegistrationManager.Service> activeServices,
                            final String systemId) {
        AuthenticateOuterClass.GetAccessTokenRequest request = AuthenticateOuterClass.GetAccessTokenRequest.newBuilder()
                .addAllServices(activeServices.stream().map(RegistrationManager.Service::name).collect(Collectors.toList()))
                .setSystemUuid(systemId)
                .build();
        AuthenticateOuterClass.GetAccessTokenResponse response = this.grpc.get().getAccessToken(request);
        return response.getToken();
    }
}
