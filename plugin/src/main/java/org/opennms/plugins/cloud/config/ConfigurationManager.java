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

import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.AUTHENTCATED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.CONFIGURED;
import static org.opennms.plugins.cloud.config.SecureCredentialsVaultUtil.TOKEN_KEY;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jline.utils.Log;
import org.opennms.dataplatform.access.AuthenticateGrpc;
import org.opennms.integration.api.v1.runtime.RuntimeInfo;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.plugins.cloud.config.SecureCredentialsVaultUtil.Type;
import org.opennms.plugins.cloud.grpc.GrpcConnection;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.plugins.cloud.srv.RegistrationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationManager {

    /** See also <a href="https://confluence.internal.opennms.com/pages/viewpage.action?spaceKey=PRODDEV&title=High+Level+Message+Sequencing+-+System+Authorization">...</a>. */
    public enum ConfigStatus {
        /** We never tried to configure the cloud plugin. */
        NOT_ATTEMPTED,
        /** The cloud plugin is successfully authenticated (Step 5). */
        AUTHENTCATED,
        /** The cloud plugin is configured successfully. (Step 7, 9, 10) */
        CONFIGURED,
        /** The cloud plugin is configured but the configuration failed. */
        FAILED
    }

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);

    private ConfigStatus currentStatus = ConfigStatus.NOT_ATTEMPTED;

    private final SecureCredentialsVaultUtil scv;

    private final RegistrationManager serviceManager;
    private final List<GrpcService> grpcServices;

    private final GrpcConnectionConfig pasConfigTls;
    private final GrpcConnectionConfig pasConfigMtls;
    private final RuntimeInfo runtimeInfo;

    public ConfigurationManager(final SecureCredentialsVault scv,
                                final GrpcConnectionConfig pasConfigTls,
                                final GrpcConnectionConfig pasConfigMtls,
                                final RegistrationManager serviceManager,
                                final RuntimeInfo runtimeInfo,
                                final List<GrpcService> grpcServices
                                ) {
        this.scv = new SecureCredentialsVaultUtil(Objects.requireNonNull(scv));
        this.pasConfigTls = Objects.requireNonNull(pasConfigTls);
        this.pasConfigMtls = Objects.requireNonNull(pasConfigMtls);
        this.serviceManager = Objects.requireNonNull(serviceManager);
        this.grpcServices = Objects.requireNonNull(grpcServices);
        this.runtimeInfo = Objects.requireNonNull(runtimeInfo);

        boolean importedFromZip = importCloudCredentialsIfPresent();
        if (!importedFromZip
                // the authentication has been done previously => lets configure and start services
                && ( AUTHENTCATED.name().equals(this.scv.getOrNull(SecureCredentialsVaultUtil.Type.configstatus))
                  || CONFIGURED.name().equals(this.scv.getOrNull(SecureCredentialsVaultUtil.Type.configstatus)))) {
            configure();
        }
    }

    boolean importCloudCredentialsIfPresent() {
        boolean importedFromZipFile = false;
        Path cloudCredentialsFile = Path.of(System.getProperty("opennms.home") + "/etc/cloud-credentials.zip");
        if (Files.exists(cloudCredentialsFile)) {
            try {
                CertificateImporter importer = new CertificateImporter(
                        cloudCredentialsFile.toString(),
                        scv,
                        pasConfigTls);
                importer.doIt();

                GrpcConnectionConfig cloudGatewayConfig = readCloudGatewayConfig().toBuilder()
                        .tokenKey(TOKEN_KEY)
                        .tokenValue(scv.getOrNull(Type.tokenvalue))
                        .security(GrpcConnectionConfig.Security.MTLS)
                        .build();

                initGrpcServices(cloudGatewayConfig); // give all grpc services the new config
                LOG.info("All services configured with grpc config.");

                activateServices(Collections.singleton(RegistrationManager.Service.TSAAS));
                LOG.info("Active services registered with OpenNMS.");

                importedFromZipFile = true;

            } catch (Exception e) {
                LOG.warn("Could not import {}. Will continue with old credentials.", cloudCredentialsFile, e);
            }
        }
        return importedFromZipFile;
    }

    /**
     * See also: <a href="https://confluence.internal.opennms.com/pages/viewpage.action?spaceKey=PRODDEV&title=High+Level+Message+Sequencing+-+System+Authorization">...</a>
     * This is step
     * 5.) authenticate(String opennmsKey, environment-uuid, system-uuid) return cert, grpc endpoint
     */
    public void initConfiguration(final String key) {
        if(!PrerequisiteChecker.isSystemIdOk(this.runtimeInfo.getSystemId())) {
            LOG.error("Cannot initConfiguration, please fix systemId first!");
            this.currentStatus = ConfigStatus.FAILED;
            return;
        }
        try {
            Objects.requireNonNull(key);

            LOG.info("Starting configuration of cloud connection.");

            // Fetching initial credentials via TLS and cloud key
            GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> grpcWithTls = new GrpcConnection<>(pasConfigTls,
                    AuthenticateGrpc::newBlockingStub);
            final PasAccess pasWithTls = new PasAccess(grpcWithTls);
            Map<SecureCredentialsVaultUtil.Type, String> cloudCredentials = pasWithTls.fetchCredentialsFromAccessService(key, runtimeInfo.getSystemId());
            LOG.info("Cloud configuration received from PAS (Platform Access Service).");
            cloudCredentials.put(Type.configstatus, AUTHENTCATED.name());
            scv.putProperties(cloudCredentials);
            LOG.info("Cloud configuration stored in OpenNMS.");
        } catch (Exception e) {
            this.currentStatus = ConfigStatus.FAILED;
            LOG.error("configure failed.", e);
            throw e;
        }
    }

    /**
     * See also: <a href="https://confluence.internal.opennms.com/pages/viewpage.action?spaceKey=PRODDEV&title=High+Level+Message+Sequencing+-+System+Authorization">...</a>
     * These are the steps
     * // 7.) identity:
     * // 9.) getServices
     * // 10.) getAccessToken (cert, system-uuid, service) return token
     */
    public ConfigStatus configure() {
        if(!PrerequisiteChecker.isSystemIdOk(this.runtimeInfo.getSystemId())) {
            LOG.error("Cannot configure cloud connection, please fix systemId first!");
            this.currentStatus = ConfigStatus.FAILED;
            return this.currentStatus;
        }
        try {
            String systemId = runtimeInfo.getSystemId();
            GrpcConnectionConfig cloudGatewayConfig = readCloudGatewayConfig();
            GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> pasWithMtlsConfig = createPasGrpc(cloudGatewayConfig);
            final PasAccess pasWithMtls = new PasAccess(pasWithMtlsConfig);

            // step 7: identify
            // as discussed in the Green Twine meeting: we skip this call for now. Not necessary for TSAAS.

            // step 8: getServices
            Set<RegistrationManager.Service> activeServices = pasWithMtls.getActiveServices(systemId);
            String activeServicesAsString = activeServices.stream()
                    .map(Enum::name)
                    .collect(Collectors.joining(","));
            LOG.info("Active services received: {}.", activeServicesAsString);

            // step 10: getAccessToken
            final String token = pasWithMtls.getToken(activeServices, systemId);
            cloudGatewayConfig = cloudGatewayConfig.toBuilder()
                    .tokenKey(TOKEN_KEY)
                    .tokenValue(token)
                    .security(GrpcConnectionConfig.Security.MTLS)
                    .build();
            LOG.info("Received token.");

            initGrpcServices(cloudGatewayConfig); // give all grpc services the new config
            LOG.info("All services configured with grpc config.");

            activateServices(activeServices);
            LOG.info("Active services registered with OpenNMS.");

            this.currentStatus = CONFIGURED; // this is a transient state so we don't save it in scv
        } catch (Exception e) {
            this.currentStatus = ConfigStatus.FAILED;
            LOG.error("configure failed.", e);
        }
        return this.currentStatus;
    }

    GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> createPasGrpc(final GrpcConnectionConfig cloudGatewayConfig) {
        GrpcConnectionConfig mtlsConfig = pasConfigMtls.toBuilder()
                .privateKey(cloudGatewayConfig.getPrivateKey())
                .publicKey(cloudGatewayConfig.getPublicKey())
                .build();
        return new GrpcConnection<>(
                mtlsConfig,
                AuthenticateGrpc::newBlockingStub);
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

    GrpcConnectionConfig readCloudGatewayConfig() {
        return GrpcConnectionConfig.builder()
                .host(this.scv.getOrNull(SecureCredentialsVaultUtil.Type.grpchost))
                .port(this.scv.get(SecureCredentialsVaultUtil.Type.grpcport).map(Integer::parseInt).orElse(0))
                .privateKey(this.scv.getOrNull(SecureCredentialsVaultUtil.Type.privatekey))
                .publicKey(this.scv.getOrNull(SecureCredentialsVaultUtil.Type.publickey))
                .tokenKey(this.scv.getOrNull(SecureCredentialsVaultUtil.Type.tokenkey))
                .tokenValue(this.scv.getOrNull(SecureCredentialsVaultUtil.Type.tokenvalue))
                .build();
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
