package org.opennms.plugins.cloud.tsaas;

import java.util.Objects;

public class TsaasConfig {

  private final String host;
  private final int port;
  private final String clientToken;
  private final boolean mtlsEnabled;
  private final String certificateDir;

  public TsaasConfig(final String host, final int port, final String clientToken, final boolean mtlsEnabled, final String certificateDir) {
    this.host = Objects.requireNonNull(host);
    this.port = requirePositiveNumber(port);
    this.clientToken = Objects.requireNonNull(clientToken);
    this.mtlsEnabled = mtlsEnabled;
    this.certificateDir = this.mtlsEnabled ? Objects.requireNonNull(certificateDir) : certificateDir;
  }

  private int requirePositiveNumber(int value) {
    if (value < 0) {
      throw new IllegalArgumentException(String.format("A positive number is required but was %s", value));
    }
    return value;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getClientToken() {
    return clientToken;
  }

  public boolean isMtlsEnabled() {
    return mtlsEnabled;
  }

  public String getCertificateDir() {
    return this.certificateDir;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Builder() {}
    private String host = "localhost";
    private int port = 5001;
    private String clientToken = "TOKEN";
    private boolean mtlsEnabled = false;
    private String certificateDir;

    public TsaasConfig build() {
      return new TsaasConfig(
          host,
          port,
          clientToken,
          mtlsEnabled,
          certificateDir);
    }

    public Builder host(final String host) {
      this.host = host;
      return this;
    }

    public Builder port(final int port) {
      this.port = port;
      return this;
    }

    public Builder clientToken(final String clientToken) {
      this.clientToken = clientToken;
      return this;
    }

    public Builder mtlsEnabled(final boolean mtlsEnabled) {
      this.mtlsEnabled = mtlsEnabled;
      return this;
    }


    public Builder certificateDir(final String certificateDir) {
      this.certificateDir = certificateDir;
      return this;
    }
  }
}
