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

package org.opennms.plugins.cloud.tsaas;

import java.util.Objects;

public class TsaasConfig {

  private final String host;
  private final int port;
  private final String tokenKey;
  private final boolean mtlsEnabled;
  private final int batchSize;
  private final long maxBatchWaitTimeInMilliSeconds;

  public TsaasConfig(final String host,
      final int port,
      final String tokenKey,
      final boolean mtlsEnabled,
      final int batchSize,
      final long maxBatchWaitTimeInMilliSeconds) {
    this.host = Objects.requireNonNull(host);
    this.port = requirePositiveNumber(port);
    this.tokenKey = Objects.requireNonNull(tokenKey);
    this.mtlsEnabled = mtlsEnabled;
    this.batchSize = batchSize;
    this.maxBatchWaitTimeInMilliSeconds = maxBatchWaitTimeInMilliSeconds;
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

  public boolean isMtlsEnabled() {
    return mtlsEnabled;
  }

  public int getBatchSize() {
    return this.batchSize;
  }

  public long getMaxBatchWaitTimeInMilliSeconds() {
    return this.maxBatchWaitTimeInMilliSeconds;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Builder() {}
    private String host = "localhost";
    private int port = 5001;
    private String tokenKey = "token";
    private boolean mtlsEnabled = false;
    private int batchSize = 1000;
    private long maxBatchWaitTimeInMilliSeconds = 5000;

    public TsaasConfig build() {
      return new TsaasConfig(
          host,
          port,
          tokenKey,
          mtlsEnabled,
          batchSize,
          maxBatchWaitTimeInMilliSeconds);
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

    public Builder mtlsEnabled(final boolean mtlsEnabled) {
      this.mtlsEnabled = mtlsEnabled;
      return this;
    }

    public Builder batchSize(int batchSize) {
      this.batchSize = batchSize;
      return this;
    }

    public Builder maxBatchWaitTimeInMilliSeconds(long maxBatchWaitTimeInMilliSeconds) {
      this.maxBatchWaitTimeInMilliSeconds = maxBatchWaitTimeInMilliSeconds;
      return this;
    }
  }
}
