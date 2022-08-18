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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jline.utils.Log;
import org.opennms.dataplatform.access.AuthenticateGrpc;
import org.opennms.dataplatform.access.AuthenticateOuterClass;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.plugins.cloud.grpc.GrpcConnection;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.plugins.cloud.srv.RegistrationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

public class ConfigurationManager {

    public enum ConfigStatus {
        /** We never tried to configure the cloud plugin. */
        NOT_ATTEMPTED,
        /** The cloud plugin is configured successfully. */
        SUCCESSFUL,
        /** The cloud plugin is configured but the configuration failed. */
        FAILED
    }

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);

    final String systemId =  UUID.randomUUID().toString(); // TODO: Patrick get from OpenNMS
    private ConfigStatus currentStatus = ConfigStatus.NOT_ATTEMPTED;

    private final SecureCredentialsVaultUtil scv;

    private final RegistrationManager serviceManager;
    private final List<GrpcService> grpcServices;

    private final AuthenticateGrpc.AuthenticateBlockingStub grpc;

    private final GrpcConnectionConfig pasConfigTls;
    private final GrpcConnectionConfig pasConfigMtls;

    public ConfigurationManager(final SecureCredentialsVault scv,
                                final GrpcConnectionConfig pasConfigTls,
                                final GrpcConnectionConfig pasConfigMtls,
                                final RegistrationManager serviceManager,
                                final List<GrpcService> grpcServices
                                ) {
        this(scv,
                pasConfigTls,
                pasConfigMtls,
                serviceManager,
                grpcServices,
                new GrpcConnection<>(pasConfigTls,
                        AuthenticateGrpc::newBlockingStub).get());
    }

    public ConfigurationManager(final SecureCredentialsVault scv,
                                final GrpcConnectionConfig pasConfigTls,
                                final GrpcConnectionConfig pasConfigMtls,
                                final RegistrationManager serviceManager,
                                final List<GrpcService> grpcServices,
                                final AuthenticateGrpc.AuthenticateBlockingStub grpc) {
        this.scv = new SecureCredentialsVaultUtil(Objects.requireNonNull(scv));
        this.pasConfigTls = Objects.requireNonNull(pasConfigTls);
        this.pasConfigMtls = Objects.requireNonNull(pasConfigMtls);
        this.serviceManager = Objects.requireNonNull(serviceManager);
        this.grpcServices = Objects.requireNonNull(grpcServices);
        this.grpc = Objects.requireNonNull(grpc);
        importCloudCredentialsIfPresent();
    }

    void importCloudCredentialsIfPresent() {
        Path cloudCredentialsFile = Path.of(System.getProperty("opennms.home") + "/etc/cloud-credentials.zip");
        if (Files.exists(cloudCredentialsFile)) {
            try {
                CertificateImporter importer = new CertificateImporter(
                        cloudCredentialsFile.toString(),
                        scv,
                        pasConfigTls,
                        this);
                importer.doIt();
            } catch (Exception e) {
                LOG.warn("Could not import {}. Will continue with old credentials.", cloudCredentialsFile, e);
            }
        }
    }

    /**
     * See also: <a href="https://confluence.internal.opennms.com/pages/viewpage.action?spaceKey=PRODDEV&title=High+Level+Message+Sequencing+-+System+Authorization">...</a>
     * // 5.) authenticate(String opennmsKey, environment-uuid, system-uuid) return cert, grpc endpoint
     * // 6.) store: cert, cloud services, environment-uuid
     * // 7.) identity:
     * // 9.) getServices
     * // 10.) getAccessToken (cert, system-uuid, service) return token
     */
    public void configure(final String key) {
        try {
            Objects.requireNonNull(key);

            LOG.info("Starting configuration of cloud connection.");

            // Fetching initial credentials via TLS and cloud key
            GrpcConnectionConfig cloudGatewayConfig = fetchCredentialsFromAccessService(key);
            LOG.info("Cloud configuration received from PAS (Platform Access Service).");

            storeCredentials(cloudGatewayConfig);
            LOG.info("Cloud configuration stored in OpenNMS.");

            // From now on we need to communicate via MTLS
            GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> grpcWithMtls = createGrpcWithMtls(cloudGatewayConfig);
            Set<RegistrationManager.Service> activeServices = getActiveServices(grpcWithMtls.get());
            String activeServicesAsString = activeServices.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
            LOG.info("Active services received: {}.", activeServicesAsString);

            scv.putProperty(SecureCredentialsVaultUtil.Type.activeservices, activeServicesAsString);
            LOG.info("Active services stored in OpenNMS.");

            initGrpcServices(cloudGatewayConfig); // give all grpc services the new config
            LOG.info("All services configured with grpc config.");

            activateServices(activeServices);
            LOG.info("Active services registered with OpenNMS.");
            this.currentStatus = ConfigStatus.SUCCESSFUL;
        } catch (Exception e) {
            this.currentStatus = ConfigStatus.FAILED;
            LOG.error("configure failed.", e);
            throw e;
        }
    }

    GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> createGrpcWithMtls(final GrpcConnectionConfig cloudGatewayConfig) {
        GrpcConnectionConfig mtlsConfig = pasConfigMtls.toBuilder()
                .privateKey(cloudGatewayConfig.getPrivateKey())
                .publicKey(cloudGatewayConfig.getPublicKey())
                .build();
        return new GrpcConnection<>(
                mtlsConfig,
                AuthenticateGrpc::newBlockingStub);
    }

    private Set<RegistrationManager.Service> getActiveServices(final AuthenticateGrpc.AuthenticateBlockingStub grpc) {
        AuthenticateOuterClass.GetServicesResponse servicesResponse = grpc
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

    /** Registers the active services with OpenNMS. */
    private void activateServices(final Set<RegistrationManager.Service> activeServices){
        Set<RegistrationManager.Service> inactiveServices =  new HashSet<>(Arrays.asList(RegistrationManager.Service.values()));
        inactiveServices.removeAll(activeServices);
        for(RegistrationManager.Service service : inactiveServices ) {
            this.serviceManager.deregister(service);
        }
        for(RegistrationManager.Service service : activeServices ) {
            this.serviceManager.register(service);
        }
    }

    @VisibleForTesting
    GrpcConnectionConfig fetchCredentialsFromAccessService(final String key) {

        AuthenticateOuterClass.AuthenticateKeyRequest keyRequest = AuthenticateOuterClass.AuthenticateKeyRequest.newBuilder()
                .setAuthenticationKey(key)
                .setSystemUuid(systemId)
                .build();
        AuthenticateOuterClass.AuthenticateKeyResponse response = grpc.authenticateKey(keyRequest);

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

    @VisibleForTesting
    void storeCredentials(final GrpcConnectionConfig config) {
        Map<SecureCredentialsVaultUtil.Type, String> attributes = new EnumMap<>(SecureCredentialsVaultUtil.Type.class);
        attributes.put(SecureCredentialsVaultUtil.Type.privatekey, config.getPrivateKey());
        attributes.put(SecureCredentialsVaultUtil.Type.publickey, config.getPublicKey());
        attributes.put(SecureCredentialsVaultUtil.Type.tokenkey, config.getTokenKey());
        attributes.put(SecureCredentialsVaultUtil.Type.tokenvalue, config.getTokenValue());
        attributes.put(SecureCredentialsVaultUtil.Type.grpchost, config.getHost());
        attributes.put(SecureCredentialsVaultUtil.Type.grpcport, Integer.toString(config.getPort()));
        this.scv.putProperties(attributes);
    }

    public void initGrpcServices(final GrpcConnectionConfig config) {
        for(GrpcService service: grpcServices) {
            try {
                service.initGrpc(config);
            } catch (Exception e) {
                Log.error("could not initGrpc", e);
            }
        }
    }

    public ConfigStatus getStatus() {
        return currentStatus;
    }
}
