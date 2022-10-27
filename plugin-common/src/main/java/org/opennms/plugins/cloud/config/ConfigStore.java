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

import java.util.Map;
import java.util.Optional;

/**
 * A thin wrapper around the SecureCredentialsVault.
 * Makes the accessing properties easier.
 */
public interface ConfigStore {
    String STORE_PREFIX = "plugin.cloud";
    String TOKEN_KEY = "token";

    /**
     * All enums must be lower case.
     * Otherwise scv won't save them correctly.
     */
    @SuppressWarnings("java:S115") // we don't want to be warned about lower case, upper case is not working for SCV
    enum Key {
        truststore, publickey, privatekey, tokenkey, tokenvalue, grpchost, grpcport,
        activeservices,
        /**
         * Defines if plugin was already configured via ConfigurationManager.
         * See ConfigurationManager.ConfigStatus
         */
        configstatus
    }

    String getOrNull(Key type);

    Optional<String> get(Key type);

    void putProperty(final Key key, String value);

    void putProperties(final Map<Key, String> properties);

}
