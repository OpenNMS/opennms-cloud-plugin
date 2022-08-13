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

package org.opennms.plugins.cloud.srv.tsaas;

import java.util.Objects;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class TsaasConfig {

    private final String host;
    private final int port;
    private final String tokenKey;
    private final String tokenValue;
    private final boolean mtlsEnabled;
    private final int batchSize;
    private final long maxBatchWaitTimeInMilliSeconds;

    public TsaasConfig(
            final String host,
            final int port,
            final String tokenKey,
            final String tokenValue,
            final boolean mtlsEnabled,
            final int batchSize,
            final long maxBatchWaitTimeInMilliSeconds) {
        this.host = Objects.requireNonNull(host);
        this.port = requirePositiveNumber(port);
        this.tokenKey = Objects.requireNonNull(tokenKey);
        this.tokenValue = Objects.requireNonNull(tokenValue);
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

    public static class TsaasConfigBuilder {
        private String host = "localhost";
        private int port = 5001;
        private String tokenKey = "token";
        private String tokenValue = "acme";
        private boolean mtlsEnabled = false;
        private int batchSize = 1000;
        private long maxBatchWaitTimeInMilliSeconds = 5000;
    }


}
