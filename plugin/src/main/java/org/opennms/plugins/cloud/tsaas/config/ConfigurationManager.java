package org.opennms.plugins.cloud.tsaas.config;

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

import static org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.SCV_ALIAS;
import static org.opennms.plugins.cloud.tsaas.config.ConfigurationManager.ConfigStatus.SUCCESSFUL;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.opennms.dataplatform.access.AuthenticateGrpc;
import org.opennms.dataplatform.access.AuthenticateOuterClass;
import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.scv.immutables.ImmutableCredentials;
import org.opennms.plugins.cloud.tsaas.GrpcConnection;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil;
import org.opennms.plugins.cloud.tsaas.TsaasConfig;

// 5.) authenticate(String opennmsKey, environment-uuid, system-uuid) return cert, grpc endpoint
// 6.) store: cert, cloud services, environment-uuid
// 7.) identity:
// 9.) getServices
// 10.) getAccessToken (cert, system-uuid, service) return token
public class ConfigurationManager {

    public enum ConfigStatus {
        /** We never tried to configure the cloud plugin. */
        NOT_ATTEMPTED,
        /** The cloud plugin is configured successfully. */
        SUCCESSFUL,
        /** The cloud plugin is configured but the configuration failed. */
        FAILED

    }

    private ConfigStatus currentStatus = ConfigStatus.NOT_ATTEMPTED;

    private final SecureCredentialsVault scv;
    private final TsaasConfig config;

    public ConfigurationManager(final SecureCredentialsVault scv, final TsaasConfig config) {
        this.scv = Objects.requireNonNull(scv);
        this.config = Objects.requireNonNull(config);
    }

    public void configure(final String key) {
        Objects.requireNonNull(key);
        SecureCredentialsVaultUtil scvUtil = new SecureCredentialsVaultUtil(scv);
        final GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> grpcConnection =
                new GrpcConnection<>(config, scvUtil, AuthenticateGrpc::newBlockingStub);
        AuthenticateGrpc.AuthenticateBlockingStub grpc = grpcConnection.get();

        AuthenticateOuterClass.AuthenticateKeyRequest keyRequest = AuthenticateOuterClass.AuthenticateKeyRequest.newBuilder()
                .setAuthenticationKey(key)
                .setSystemUuid(UUID.randomUUID().toString()) // TODO Patrick: get that from OpenNMS
                .build();
        AuthenticateOuterClass.AuthenticateKeyResponse response = grpc.authenticateKey(keyRequest);
        CloudGatewayConfig config = CloudGatewayConfig.builder()
                .publicKey(response.getCertificate())
                .privateKey(response.getPrivateKey())
                .securedGrpcEndpoint(response.getGrpcEndpoint())
                .build();
       importCredentials(config);

        // TODO: actual configuration
        this.currentStatus = SUCCESSFUL;
    }

    public void importCredentials(final CloudGatewayConfig config) {

        // retain old values if present
        Map<String, String> attributes = new HashMap<>();
        SecureCredentialsVaultUtil scvUtil = new SecureCredentialsVaultUtil(scv);
        scvUtil.getCredentials()
                .map(Credentials::getAttributes)
                .map(Map::entrySet)
                .stream()
                .flatMap(Set::stream)
                .forEach(e -> attributes.put(e.getKey(), e.getValue()));

        // add / override new value
        attributes.put(SecureCredentialsVaultUtil.Type.privatekey.name(), config.getPrivateKey());
        attributes.put(SecureCredentialsVaultUtil.Type.publickey.name(), config.getPublicKey());
        attributes.put(SecureCredentialsVaultUtil.Type.token.name(), config.getToken());

        // Store modified credentials
        Credentials newCredentials = new ImmutableCredentials("", "", attributes);
        scv.setCredentials(SCV_ALIAS, newCredentials);
    }



    public ConfigStatus getStatus() {
        return currentStatus;
    }

}
