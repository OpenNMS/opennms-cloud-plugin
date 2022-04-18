package org.opennms.plugins.cloud.tsaas;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.opennms.integration.api.v1.scv.Credentials;
import org.opennms.integration.api.v1.scv.SecureCredentialsVault;

public class SecureCredentialsVaultUtil {
  public static final String SCV_ALIAS = "plugin.cloud.tsaas";

  public enum Type {
    truststore, publickey, privatekey;
    public static boolean isValid(String value) {
      return Arrays.stream(Type.values()).anyMatch(e -> e.name().equals(value));
    }
  }

  private final SecureCredentialsVault scv;

  public SecureCredentialsVaultUtil(SecureCredentialsVault scv) {
    this.scv = Objects.requireNonNull(scv);
  }

  public Optional<Credentials> getCredentials() {
    return Optional.ofNullable(this.scv.getCredentials(SCV_ALIAS));
  }
}
