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

package org.opennms.plugins.cloud.grpc;

import static org.opennms.plugins.cloud.config.ConfigStore.TOKEN_KEY;

import java.util.Objects;

import lombok.Builder;
import lombok.Data;

@Builder(toBuilder = true)
@Data
public class GrpcConnectionConfig {

    public enum Security {
        TLS,
        MTLS
    }

    private final String host;
    private final int port;
    private final String tokenKey;
    private final String tokenValue;
    private final Security security;

    private final String publicKey;
    private final String privateKey;
    private final String clientTrustStore;

    /** Called by blueprint.xml. */
    public GrpcConnectionConfig(
            final String host,
            final int port,
            final Security security,
            final String clientTrustStore) {
        this(
                host,
                port,
                null,
                null,
                security,
                null,
                null,
                clientTrustStore);
    }

    @SuppressWarnings("java:S107") // this constructor is only used by builder => ok to have many parameters
    private GrpcConnectionConfig(
            final String host,
            final int port,
            final String tokenKey,
            final String tokenValue,
            final Security security,
            final String publicKey,
            final String privateKey,
            final String clientTrustStore) {
        this.host = Objects.requireNonNull(host);
        this.port = requirePositiveNumber(port);
        this.tokenKey = tokenKey;
        this.tokenValue = tokenValue;
        this.security = security;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.clientTrustStore = clientTrustStore;
    }

    private int requirePositiveNumber(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(String.format("A positive number is required but was %s", value));
        }
        return value;
    }

    @SuppressWarnings("java:S1068") // fields are not unused but part of lombok builder
    public static class GrpcConnectionConfigBuilder {
        private String host = "localhost"; // default value
        private String tokenKey = TOKEN_KEY; // default value
        private Security security = Security.TLS; // default value
    }
}
