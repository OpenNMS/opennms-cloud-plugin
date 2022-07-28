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


package org.opennms.plugins.cloud.tsaas.config;

import static org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.SCV_ALIAS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.scv.immutables.ImmutableCredentials;
import org.opennms.plugins.cloud.config.CloudConfig;
import org.opennms.plugins.cloud.tsaas.GrpcConnection;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.Type;
import org.opennms.tsaas.Tsaas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Imports certificates to be used with cloud tsaas. */
public class CertificateImporter {

    private final String fileParam;

    private final CloudConfig config;

    private final SecureCredentialsVault scv;

    private static final Logger LOG = LoggerFactory.getLogger(CertificateImporter.class);

    public CertificateImporter(final String fileParam,
                               final SecureCredentialsVault scv,
                               final CloudConfig config) {
        this.fileParam = Objects.requireNonNull(fileParam);
        this.scv = Objects.requireNonNull(scv);
        this.config = Objects.requireNonNull(config);
    }

    public void doIt() throws IOException {

        // Validate
        Path file = Path.of(fileParam);
        if (!Files.isRegularFile(file)) {
            LOG.info("{} is not a file.", fileParam);
            return;
        }
        if (!Files.isReadable(file)) {
            LOG.info("{} is not a readable.", fileParam);
            return;
        }

        final ConfigZipExtractor configZip = new ConfigZipExtractor(file);

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
        attributes.put(Type.privatekey.name(), configZip.getPrivateKey());
        attributes.put(Type.publickey.name(), configZip.getPublicKey());
        attributes.put(Type.token.name(), configZip.getJwtToken());

        // Store modified credentials
        Credentials newCredentials = new ImmutableCredentials("", "", attributes);
        scv.setCredentials(SCV_ALIAS, newCredentials);
        LOG.info("Imported certificates from {}", fileParam);

        // Check if storage worked
        Credentials credFromScv = scv.getCredentials(SCV_ALIAS);
        if (Objects.equals(configZip.getPrivateKey(), credFromScv.getAttribute(Type.privatekey.name()))
                && Objects.equals(configZip.getPublicKey(), credFromScv.getAttribute(Type.publickey.name()))
                && Objects.equals(configZip.getJwtToken(), credFromScv.getAttribute(Type.token.name()))) {
            LOG.info("Storing of certificates was successfully, will delete zip file.");

            Files.delete(file);
        } else {
            LOG.info("Storing of certificates was NOT successfully!!!");
            LOG.info("Will abort.");
            return;
        }

        // Check if the connection can be established
        LOG.info("Checking if connection to server works");
        try {
            GrpcConnection grpc = new GrpcConnection(this.config, scvUtil);
            Tsaas.CheckHealthResponse response = grpc.get().checkHealth(Tsaas.CheckHealthRequest.newBuilder().build());
            LOG.info("Connection to cloud server: OK");
            LOG.info("Status of cloud server: {}", response.getStatus().name());
        } catch (Exception e) {
            LOG.info("Warning: Connection to cloud was not successful: {}", e.getMessage());
        }
    }
}
