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

package org.opennms.plugins.cloud.config;

import java.util.Objects;

import lombok.Builder;
import lombok.Data;

/** Configuration for Platform Access Service */
@Builder(toBuilder = true)
@Data
public class PasConfig {

    private final String host;
    private final int port;

    public PasConfig(
            final String host,
            final int port) {
        this.host = Objects.requireNonNull(host);
        this.port = requirePositiveNumber(port);
    }

    private int requirePositiveNumber(int value) {
        if (value < 0) {
            throw new IllegalArgumentException(String.format("A positive number is required but was %s", value));
        }
        return value;
    }

    public static class PasConfigBuilder {
        private String host = "localhost";
        private int port = 5001;
    }
}
