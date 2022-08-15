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

import static org.opennms.plugins.cloud.srv.tsaas.SecureCredentialsVaultUtil.SCV_ALIAS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.plugins.cloud.grpc.GrpcConnection;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.tsaas.SecureCredentialsVaultUtil;
import org.opennms.plugins.cloud.srv.tsaas.SecureCredentialsVaultUtil.Type;
import org.opennms.tsaas.TimeseriesGrpc;
import org.opennms.tsaas.Tsaas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Imports certificates to be used with cloud tsaas. */
public class CertificateImporter {

    private final String fileParam;

    private final GrpcConnectionConfig config;

    private final SecureCredentialsVault scv;

    private final ConfigurationManager configManager;

    private static final Logger LOG = LoggerFactory.getLogger(CertificateImporter.class);

    public CertificateImporter(final String fileParam,
                               final SecureCredentialsVault scv,
                               final GrpcConnectionConfig config,
                               final ConfigurationManager configManager) {
        this.fileParam = Objects.requireNonNull(fileParam);
        this.scv = Objects.requireNonNull(scv);
        this.config = Objects.requireNonNull(config);
        this.configManager = Objects.requireNonNull(configManager);
    }

    public void doIt() throws IOException {

        // Validate
        Path file = Path.of(fileParam);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException(String.format("%s is not a file.", fileParam));
        }
        if (!Files.isReadable(file)) {
            throw new IllegalArgumentException(String.format("%s is not a readable.", fileParam));
        }

        final GrpcConnectionConfig cloudGatewayConfig = new ConfigZipExtractor(file).getGrpcConnectionConfig();
        configManager.storeCredentials(cloudGatewayConfig, ""); // per default we activate no services
        LOG.info("Imported certificates from {}", fileParam);

        if (isConfigStored(cloudGatewayConfig)) {
            LOG.info("Storing of certificates was successfully, will delete zip file.");
            Files.delete(file);
        } else {
            LOG.info("Storing of certificates was NOT successfully!!!");
            LOG.info("Will abort.");
        }

        tryConnection(scv);
    }

    public boolean isConfigStored(final GrpcConnectionConfig config) {
        Credentials credFromScv = scv.getCredentials(SCV_ALIAS);
        return Objects.equals(config.getPrivateKey(), credFromScv.getAttribute(Type.privatekey.name()))
                && Objects.equals(config.getPublicKey(), credFromScv.getAttribute(Type.publickey.name()))
                && Objects.equals(config.getTokenValue(), credFromScv.getAttribute(Type.tokenvalue.name()));
    }

    boolean tryConnection(final SecureCredentialsVault scv) {
        // Check if the connection can be established
        LOG.info("Checking if connection to server works");
        try {
            SecureCredentialsVaultUtil scvUtil = new SecureCredentialsVaultUtil(scv);
            GrpcConnection<TimeseriesGrpc.TimeseriesBlockingStub> grpc = new GrpcConnection<>(this.config, TimeseriesGrpc::newBlockingStub);
            Tsaas.CheckHealthResponse response = grpc.get().checkHealth(Tsaas.CheckHealthRequest.newBuilder().build());
            LOG.info("Connection to cloud server: OK");
            LOG.info("Status of cloud server: {}", response.getStatus().name());
        } catch (Exception e) {
            LOG.info("Warning: Connection to cloud was not successful: {}", e.getMessage());
            return false;
        }
        return true;
    }
}
