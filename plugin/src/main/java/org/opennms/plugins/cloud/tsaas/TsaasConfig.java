package org.opennms.plugins.cloud.tsaas;

import java.util.Objects;

public class TsaasConfig {

  private final String host;
  private final int port;
  private final String tokenKey;
  private final String tokenValue;
  private final boolean mtlsEnabled;
  private final String certificateDir;
  private final int batchSize;

  public TsaasConfig(final String host,
      final int port,
      final String tokenKey,
      final String tokenValue,
      final boolean mtlsEnabled,
      final String certificateDir,
      final int batchSize) {
    this.host = Objects.requireNonNull(host);
    this.port = requirePositiveNumber(port);
    this.tokenKey = Objects.requireNonNull(tokenKey);
    this.tokenValue = Objects.requireNonNull(tokenValue);
    this.mtlsEnabled = mtlsEnabled;
    this.certificateDir = this.mtlsEnabled ? Objects.requireNonNull(certificateDir) : certificateDir;
    this.batchSize = batchSize;
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

  public String getTokenKey() {
    return tokenKey;
  }

  public String getTokenValue() {
    return tokenValue;
  }

  public boolean isMtlsEnabled() {
    return mtlsEnabled;
  }

  public String getCertificateDir() {
    return this.certificateDir;
  }

  public int getBatchSize() {
    return this.batchSize;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Builder() {}
    private String host = "localhost";
    private int port = 5001;
    private String tokenKey = "x-scope-orgid";
    private String tokenValue = "acme";
    private boolean mtlsEnabled = false;
    private String certificateDir;
    private int batchSize = 1000;

    public TsaasConfig build() {
      return new TsaasConfig(
          host,
          port,
          tokenKey,
          tokenValue,
          mtlsEnabled,
          certificateDir,
          batchSize);
    }

    public Builder host(final String host) {
      this.host = host;
      return this;
    }

    public Builder port(final int port) {
      this.port = port;
      return this;
    }

    public Builder tokenKey(final String tokenKey) {
      this.tokenKey = tokenKey;
      return this;
    }

    public Builder tokenValue(final String tokenValue) {
      this.tokenValue = tokenValue;
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

    public Builder batchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }
  }
}
