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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.scv.immutables.ImmutableCredentials;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.Type;

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
  SecureCredentialsVault scv;

  @Override
  public Object execute() throws Exception {

    // Validate
    Path file = Path.of(fileParam);
    if(!Files.isRegularFile(file)) {
      System.out.printf("%s is not a file.%n", fileParam);
      return null;
    }
    if(!Files.isReadable(file)) {
      System.out.printf("%s is not a readable.%n", fileParam);
      return null;
    }

    ConfigZipExtractor config = new ConfigZipExtractor(file);

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
    attributes.put(Type.privatekey.name(), config.getPrivateKey());
    attributes.put(Type.publickey.name(), config.getPublicKey());
    attributes.put(Type.token.name(), config.getJwtToken());

    // Store modified credentials
    Credentials newCredentials = new ImmutableCredentials("", "", attributes);
    scv.setCredentials(SCV_ALIAS, newCredentials);
    System.out.printf("Imported certificates from %s%n", fileParam);

    // Check if storage worked
    Credentials credFromScv = scv.getCredentials(SCV_ALIAS);
    if (Objects.equals(config.getPrivateKey(), credFromScv.getAttribute(Type.privatekey.name()))
            && Objects.equals(config.getPublicKey(), credFromScv.getAttribute(Type.publickey.name()))
            && Objects.equals(config.getJwtToken(), credFromScv.getAttribute(Type.token.name()))) {
      System.out.println("Storing of certificates was successfully, will delete zip file.");
      Files.delete(file);
    } else {
      System.out.println("Storing of certificates was NOT successfully!!!");
    }
    return null;
  }

}
