package org.opennms.plugins.cloud.tsaas.shell;

import static org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.SCV_ALIAS;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;
import org.opennms.integration.api.v1.scv.immutables.ImmutableCredentials;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil;
import org.opennms.plugins.cloud.tsaas.SecureCredentialsVaultUtil.Type;

@Command(scope = "opennms-tsaas", name = "import-cert",
    description = "Imports certificates to be used with tsaas.",
    detailedDescription= "Imports certificates to be used with tsaas."
        + "If mtls is enabled in the properties at least publickey and privatekey must be supplied."
        + "After all certificates are imported the plugin needs to be restarted to be in effect:"
        + "feature:restart cloud-plugin TODO Patrick")
@Service
public class CertificateImporter implements Action {

  @Option(name = "-t", aliases = { "--type" }, description = "Type of file, possible values = 'truststore', 'clientcert', 'clientkey'",
      required = true)
  String type;

  @Option(name="f", aliases = { "--file" }, description = "Path to file.",
      required = true)
  String fileParam;

  @Reference
  SecureCredentialsVault scv;

  @Override
  public Object execute() throws Exception {

    // Validate
    if (!Type.isValid(type)) {
        System.out.printf("Invalid parameter 'type', must be one of: %s%n", Arrays.toString(Type.values()));
        return null;
    }
    Path file = Path.of(fileParam);
    if(!Files.isRegularFile(file)) {
      System.out.printf("%s is not a file.%n", fileParam);
      return null;
    }
    if(!Files.isReadable(file)) {
      System.out.printf("%s is not a readable.%n", fileParam);
      return null;
    }

    // read file
    String value = Files.readString(file, StandardCharsets.UTF_8);

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
    attributes.put(type, value);

    // Store modified credentials
    Credentials newCredentials = new ImmutableCredentials("", "", attributes);
    scv.setCredentials(SCV_ALIAS, newCredentials);
    System.out.printf("Imported %s from %s", type, fileParam);

    return null;
  }
}
