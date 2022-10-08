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

import static org.opennms.plugins.cloud.config.ConfigStore.Key.tokenvalue;
import static org.opennms.plugins.cloud.config.ConfigStore.Key.truststore;
import static org.opennms.plugins.cloud.config.ConfigStore.TOKEN_KEY;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.AUTHENTCATED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.CONFIGURED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.FAILED;
import static org.opennms.plugins.cloud.config.PrerequisiteChecker.checkAndLogContainer;
import static org.opennms.plugins.cloud.config.PrerequisiteChecker.checkAndLogSystemId;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.opennms.dataplatform.access.AuthenticateGrpc;
import org.opennms.integration.api.v1.runtime.RuntimeInfo;
import org.opennms.plugins.cloud.config.ConfigStore.Key;
import org.opennms.plugins.cloud.grpc.CloseUtil;
import org.opennms.plugins.cloud.grpc.GrpcConnection;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.plugins.cloud.srv.RegistrationManager;
import org.opennms.plugins.cloud.srv.tsaas.TsaasStorage;
import org.opennms.tsaas.Tsaas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationManager {

    /**
     * See also <a href="https://confluence.internal.opennms.com/pages/viewpage.action?spaceKey=PRODDEV&title=High+Level+Message+Sequencing+-+System+Authorization">...</a>.
     */
    public enum ConfigStatus {
        /**
         * We never tried to configure the cloud plugin.
         */
        NOT_ATTEMPTED,
        /**
         * The cloud plugin is successfully authenticated (Step 5).
         */
        AUTHENTCATED,
        /**
         * The cloud plugin is configured successfully. (Step 7, 9, 10)
         */
        CONFIGURED,
        /**
         * The cloud plugin is configured but the configuration failed.
         */
        FAILED
    }

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);

    private ConfigStatus currentStatus = ConfigStatus.NOT_ATTEMPTED;

    private final ConfigStore config;

    private final RegistrationManager serviceManager;
    private final List<GrpcService> grpcServices;

    private final GrpcConnectionConfig pasConfigTls;
    private final GrpcConnectionConfig pasConfigMtls;
    private final RuntimeInfo runtimeInfo;

    private Instant tokenExpirationDate;
    private Instant certExpirationDate;

    public ConfigurationManager(final ConfigStore config,
                                final GrpcConnectionConfig pasConfigTls,
                                final GrpcConnectionConfig pasConfigMtls,
                                final RegistrationManager serviceManager,
                                final RuntimeInfo runtimeInfo,
                                final List<GrpcService> grpcServices
    ) {
        this.config = Objects.requireNonNull(config);
        this.pasConfigTls = Objects.requireNonNull(pasConfigTls);
        this.pasConfigMtls = Objects.requireNonNull(pasConfigMtls);
        this.serviceManager = Objects.requireNonNull(serviceManager);
        this.grpcServices = Objects.requireNonNull(grpcServices);
        this.runtimeInfo = Objects.requireNonNull(runtimeInfo);

        boolean importedFromZip = importCloudCredentialsIfPresent();
        if (!importedFromZip
                // the authentication has been done previously => lets configure and start services
                && (AUTHENTCATED.name().equals(this.config.getOrNull(Key.configstatus))
                || CONFIGURED.name().equals(this.config.getOrNull(Key.configstatus)))) {
            configure();
        }
    }

    /**
     * We keep this shortcut currently for testing purposes.
     */
    boolean importCloudCredentialsIfPresent() {
        boolean importedFromZipFile = false;
        Path cloudCredentialsFile = Path.of(System.getProperty("opennms.home") + "/etc/cloud-credentials.zip");
        if (Files.exists(cloudCredentialsFile)) {
            try {
                CertificateImporter importer = new CertificateImporter(
                        cloudCredentialsFile.toString(),
                        config);
                importer.doIt();

                GrpcConnectionConfig cloudGatewayConfig = readCloudGatewayConfig().toBuilder()
                        .tokenKey(TOKEN_KEY)
                        .tokenValue(config.getOrNull(tokenvalue))
                        .security(GrpcConnectionConfig.Security.MTLS)
                        .build();

                initGrpcServices(cloudGatewayConfig); // give all grpc services the new config
                LOG.info("All services configured with grpc config.");

                checkConnection();

                registerServices(Collections.singleton(RegistrationManager.Service.TSAAS)); // for now we enable only TSAAS via zip file.
                LOG.info("Active services registered with OpenNMS.");

                importedFromZipFile = true;
                this.currentStatus = CONFIGURED;
            } catch (Exception e) {
                this.currentStatus = FAILED;
                LOG.warn("Could not import {}.", cloudCredentialsFile, e);
            }
        }
        return importedFromZipFile;
    }

    void checkConnection() {
        Tsaas.CheckHealthResponse.ServingStatus status = grpcServices.stream()
                .filter(TsaasStorage.class::isInstance)
                .map(TsaasStorage.class::cast)
                .findFirst()
                .map(TsaasStorage::checkHealth)
                .map(Tsaas.CheckHealthResponse::getStatus)
                .orElseThrow();
        LOG.info("Status of TSAAS: {}", status); // TODO: Patrick make this more generic once we have multiple services
    }

    /**
     * See also: <a href="https://confluence.internal.opennms.com/pages/viewpage.action?spaceKey=PRODDEV&title=High+Level+Message+Sequencing+-+System+Authorization">...</a>
     * This is step
     * 5.) authenticate(String opennmsKey, environment-uuid, system-uuid) return cert, grpc endpoint
     */
    public void initConfiguration(final String key) {
        LOG.info("Starting initialization of cloud plugin.");
        checkAndLogSystemId(this.runtimeInfo.getSystemId());
        checkAndLogContainer(this.runtimeInfo);

        try (GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> grpcWithTls = new GrpcConnection<>(pasConfigTls,
                AuthenticateGrpc::newBlockingStub)) {

            Objects.requireNonNull(key, "key must not be null");
            // Fetching initial credentials via TLS and cloud key
            final PasAccess pasWithTls = new PasAccess(grpcWithTls);
            Map<ConfigStore.Key, String> cloudCredentials = pasWithTls.getCredentialsFromAccessService(key, runtimeInfo.getSystemId());
            LOG.info("Cloud configuration received from PAS (Platform Access Service).");
            if (pasConfigTls.getClientTrustStore() !=null && !pasConfigTls.getClientTrustStore().isBlank()) {
               cloudCredentials.put(truststore, pasConfigTls.getClientTrustStore()); // pass the trust store to cloud credentials
            }
            cloudCredentials.put(Key.configstatus, AUTHENTCATED.name());
            config.putProperties(cloudCredentials);
            LOG.info("Cloud configuration stored in OpenNMS.");
            this.currentStatus = AUTHENTCATED;
        } catch (Exception e) {
            this.currentStatus = ConfigStatus.FAILED;
            LOG.error("Initialization failed. Used config:\n{}", pasConfigTls, e);
            throw e;
        }
    }

    public void renewCerts() throws CertificateException {
        try (CloseUtil closeUtil = new CloseUtil()) {
            LOG.info("Starting renewing of certificates.");
            GrpcConnectionConfig cloudGatewayConfig = readCloudGatewayConfig();
            this.certExpirationDate = CertUtil.getExpiryDate(cloudGatewayConfig.getPublicKey());
            GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> pasWithMtlsConfig = createPasGrpc(cloudGatewayConfig);
            closeUtil.add(pasWithMtlsConfig);
            final PasAccess pasWithMtls = new PasAccess(pasWithMtlsConfig);
            Map<Key, String> cloudCredentials = pasWithMtls.renewCertificate(runtimeInfo.getSystemId());
            LOG.info("New certificates received from PAS (Platform Access Service).");
            cloudCredentials.put(Key.configstatus, AUTHENTCATED.name());
            config.putProperties(cloudCredentials);
            LOG.info("Cloud configuration stored in OpenNMS.");
        } catch (Exception e) {
            this.currentStatus = ConfigStatus.FAILED;
            LOG.error("fetching new certs failed.", e);
            throw e;
        }
    }

    /**
     * See also: <a href="https://confluence.internal.opennms.com/pages/viewpage.action?spaceKey=PRODDEV&title=High+Level+Message+Sequencing+-+System+Authorization">...</a>
     * These are the steps
     * // 9.) getServices
     * // 10.) getAccessToken (cert, system-uuid, service) return token
     * synchronized: its ok to call the method multiple times but we don't want it to be called at the same time (just in case).
     * It is accessed from multiple Threads, e.g. from Housekeeper
     */
    public synchronized ConfigStatus configure() {
        String systemId = runtimeInfo.getSystemId();
        try (CloseUtil closeUtil = new CloseUtil()) {
            GrpcConnectionConfig cloudGatewayConfig = readCloudGatewayConfig();
            this.certExpirationDate = CertUtil.getExpiryDate(cloudGatewayConfig.getPublicKey());
            GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> pasWithMtlsConfig = createPasGrpc(cloudGatewayConfig);
            closeUtil.add(pasWithMtlsConfig);
            final PasAccess pasWithMtls = new PasAccess(pasWithMtlsConfig);

            // step 7: identify
            // as discussed in the Green Twine meeting: we skip this call for now. Not necessary for TSAAS.

            // step 8: getServices
            Set<RegistrationManager.Service> activeServices = pasWithMtls.getActiveServices(systemId);
            LOG.info("Active services received: {}.", activeServices);

            // step 10: getAccessToken
            final String token = pasWithMtls.getToken(activeServices, systemId);
            this.tokenExpirationDate = TokenUtil.getExpiryDate(token);
            cloudGatewayConfig = cloudGatewayConfig.toBuilder()
                    .tokenKey(TOKEN_KEY)
                    .tokenValue(token)
                    .security(GrpcConnectionConfig.Security.MTLS)
                    .build();
            LOG.info("Received token.");

            initGrpcServices(cloudGatewayConfig); // give all grpc services the new config
            LOG.info("All services configured with grpc config.");
            checkConnection();

            registerServices(activeServices);
            LOG.info("Active services registered with OpenNMS.");

            this.currentStatus = CONFIGURED; // this is a transient state so we don't save it in scv
        } catch (Exception e) {
            this.currentStatus = ConfigStatus.FAILED;
            LOG.error("configure failed.", e);
        }
        return this.currentStatus;
    }

    private GrpcConnection<AuthenticateGrpc.AuthenticateBlockingStub> createPasGrpc(final GrpcConnectionConfig cloudGatewayConfig) {
        GrpcConnectionConfig mtlsConfig = pasConfigMtls.toBuilder()
                .privateKey(cloudGatewayConfig.getPrivateKey())
                .publicKey(cloudGatewayConfig.getPublicKey())
                .build();
        return new GrpcConnection<>(
                mtlsConfig,
                AuthenticateGrpc::newBlockingStub);
    }

    /**
     * Registers the active services with OpenNMS.
     */
    private void registerServices(final Set<RegistrationManager.Service> activeServices) {
        Set<RegistrationManager.Service> inactiveServices = new HashSet<>(Arrays.asList(RegistrationManager.Service.values()));
        inactiveServices.removeAll(activeServices);
        for (RegistrationManager.Service service : inactiveServices) {
            this.serviceManager.deregister(service);
        }
        for (RegistrationManager.Service service : activeServices) {
            this.serviceManager.register(service);
        }
    }

    GrpcConnectionConfig readCloudGatewayConfig() {
        return GrpcConnectionConfig.builder()
                .host(this.config.getOrNull(ConfigStore.Key.grpchost))
                .port(this.config.get(ConfigStore.Key.grpcport).map(Integer::parseInt).orElse(0))
                .privateKey(this.config.getOrNull(ConfigStore.Key.privatekey))
                .publicKey(this.config.getOrNull(ConfigStore.Key.publickey))
                .tokenKey(this.config.getOrNull(ConfigStore.Key.tokenkey))
                .tokenValue(this.config.getOrNull(Key.tokenvalue))
                .clientTrustStore(this.config.getOrNull(Key.truststore))
                .build();
    }

    public void initGrpcServices(final GrpcConnectionConfig config) {
        for (GrpcService service : grpcServices) {
            try {
                service.initGrpc(config);
            } catch (Exception e) {
                LOG.error("could not initGrpc", e);
            }
        }
    }

    public ConfigStatus getStatus() {
        return currentStatus;
    }

    public Instant getTokenExpiration() {
        return this.tokenExpirationDate == null ? Instant.now() : this.tokenExpirationDate;
    }

    public Instant getCertExpiration() {
        return this.certExpirationDate == null ? Instant.now() : this.certExpirationDate;
    }
}
