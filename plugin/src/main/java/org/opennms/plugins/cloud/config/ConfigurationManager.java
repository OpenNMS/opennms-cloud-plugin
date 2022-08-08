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

import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.SUCCESSFUL;

import java.util.Objects;

// 5.) authenticate(String opennmsKey, environment-uuid, system-uuid) return cert, grpc endpoint
// 6.) store: cert, cloud services, environment-uuid
// 7.) identity:
// 9.) getServices
// 10.) getAccessToken (cert, system-uuid, service) return token
public class ConfigurationManager {

    public enum ConfigStatus {
        /** We never tried to configure the cloud plugin. */
        NOT_ATTEMPTED,
        /** The cloud plugin is configured successfully. */
        SUCCESSFUL,
        /** The cloud plugin is configured but the configuration failed. */
        FAILED

    }

    private ConfigStatus currentStatus = ConfigStatus.NOT_ATTEMPTED;

    public void configure(final String key) {
        Objects.requireNonNull(key);
        // TODO: actual configuration
        this.currentStatus = SUCCESSFUL;
    }

    public ConfigStatus getStatus() {
        return currentStatus;
    }

}
