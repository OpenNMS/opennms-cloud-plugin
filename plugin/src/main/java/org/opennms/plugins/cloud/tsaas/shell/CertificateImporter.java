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


package org.opennms.plugins.cloud.tsaas.shell;

import static org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.SCV_ALIAS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.scv.immutables.ImmutableCredentials;
import org.opennms.plugins.cloud.tsaas.GrpcConnection;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.Type;
import org.opennms.plugins.cloud.tsaas.TsaasConfig;
import org.opennms.tsaas.Tsaas;

@Command(scope = "opennms-cloud", name = "import-cert",
    description = "Imports certificates to be used with cloud tsaas.",
    detailedDescription= "Imports certificates to be used with cloud tsaas."
        + "If mtls is enabled in the properties at least publickey and privatekey must be supplied."
        + "After all certificates are imported, the plugin needs to be restarted to be in effect.")
@Service
public class CertificateImporter implements Action {

  @Argument(name="certificateFilePath", description = "Path to file.",
      required = true)
  String fileParam;

  @Reference
  TsaasConfig config;

  @Reference
  SecureCredentialsVault scv;

  final private Consumer<String> log;

  public CertificateImporter() {
    this.log = System.out::println;
  }

  public CertificateImporter(final String fileParam,
                             final SecureCredentialsVault scv,
                             final TsaasConfig config,
                             final Consumer<String> log) {
    this.fileParam = Objects.requireNonNull(fileParam);
    this.scv = Objects.requireNonNull(scv);
    this.config = Objects.requireNonNull(config);
    this.log = Objects.requireNonNull(log);
  }

  @Override
  public Object execute() throws IOException {

    // Validate
    Path file = Path.of(fileParam);
    if(!Files.isRegularFile(file)) {
      log("%s is not a file.", fileParam);
      return null;
    }
    if(!Files.isReadable(file)) {
      log("%s is not a readable.", fileParam);
      return null;
    }

    ConfigZipExtractor configZip = new ConfigZipExtractor(file);

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
    log("Imported certificates from %s", fileParam);

    // Check if storage worked
    Credentials credFromScv = scv.getCredentials(SCV_ALIAS);
    if (Objects.equals(configZip.getPrivateKey(), credFromScv.getAttribute(Type.privatekey.name()))
            && Objects.equals(configZip.getPublicKey(), credFromScv.getAttribute(Type.publickey.name()))
            && Objects.equals(configZip.getJwtToken(), credFromScv.getAttribute(Type.token.name()))) {
      log("Storing of certificates was successfully, will delete zip file.");

      Files.delete(file);
    } else {
      log("Storing of certificates was NOT successfully!!!");
      log("Will abort.");
      return null;
    }

    // Check if the connection can be established
    log("Checking if connection to server works");
    try {
      GrpcConnection grpc = new GrpcConnection();
      grpc.init(this.config, scvUtil);
      Tsaas.CheckHealthResponse response = grpc.get().checkHealth(Tsaas.CheckHealthRequest.newBuilder().build());
      log("Connection to cloud server: OK");
      log("Status of cloud server: %s", response.getStatus().name());
    } catch(Exception e) {
      log("Warning: Connection to cloud was not successful: %s", e.getMessage());
    }

    return null;
  }

  public void log(String msg, String...s) {
    String f = String.format(msg, (String[]) s);
    this.log.accept(f);
  }
}
