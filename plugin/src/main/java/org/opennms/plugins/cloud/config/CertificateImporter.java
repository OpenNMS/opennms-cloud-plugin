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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.opennms.plugins.cloud.config.SecureCredentialsVaultUtil.Type;
import org.opennms.plugins.cloud.grpc.GrpcConnection;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.tsaas.TimeseriesGrpc;
import org.opennms.tsaas.Tsaas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Imports certificates from a Zip file for testing purposes. */
public class CertificateImporter {

    private final String fileParam;

    private final GrpcConnectionConfig config;

    private final SecureCredentialsVaultUtil scv;

    private static final Logger LOG = LoggerFactory.getLogger(CertificateImporter.class);

    public CertificateImporter(final String fileParam,
                               final SecureCredentialsVaultUtil scv,
                               final GrpcConnectionConfig config) {
        this.fileParam = Objects.requireNonNull(fileParam);
        this.scv = Objects.requireNonNull(scv);
        this.config = Objects.requireNonNull(config);
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

        final Map<Type, String> cloudGatewayConfig = new ConfigZipExtractor(file).getGrpcConnectionConfig();
        scv.putProperties(cloudGatewayConfig);
        LOG.info("Imported certificates from {}", fileParam);

        if (isConfigStored(cloudGatewayConfig)) {
            LOG.info("Storing of certificates was successfully, will delete zip file.");
            Files.delete(file);
        } else {
            LOG.info("Storing of certificates was NOT successfully!!!");
            LOG.info("Will abort.");
        }

        tryConnection();
    }

    public boolean isConfigStored(final Map<Type, String> config) {
        return Objects.equals(config.get(Type.privatekey), scv.getOrNull(Type.privatekey))
                && Objects.equals(config.get(Type.publickey), scv.getOrNull(Type.publickey))
                && Objects.equals(config.get(Type.tokenvalue), scv.getOrNull(Type.tokenvalue));
    }

    void tryConnection() {
        // Check if the connection can be established
        LOG.info("Checking if connection to server works");
        try {
            GrpcConnection<TimeseriesGrpc.TimeseriesBlockingStub> grpc = new GrpcConnection<>(this.config, TimeseriesGrpc::newBlockingStub);
            Tsaas.CheckHealthResponse response = grpc.get().checkHealth(Tsaas.CheckHealthRequest.newBuilder().build());
            LOG.info("Connection to cloud server: OK");
            LOG.info("Status of cloud server: {}", response.getStatus().name());
        } catch (Exception e) {
            LOG.info("Warning: Connection to cloud was not successful: {}", e.getMessage());
        }
    }
}
