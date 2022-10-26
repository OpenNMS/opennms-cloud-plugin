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


import org.opennms.integration.api.v1.runtime.Container;
import org.opennms.integration.api.v1.runtime.RuntimeInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrerequisiteChecker {

    private PrerequisiteChecker() {
        // Utility class
    }

    public static void checkAndLogSystemId(final String systemId) {
        if (isSystemIdOk(systemId)) {
            log.info("System id is set to {}", systemId);
        } else {
            log.warn("System id is not set up. It is advisable to set it up, see here: https://github.com/OpenNMS/opennms-cloud-plugin#system-id");
        }
    }

    static boolean isSystemIdOk(final String systemId) {
        return systemId != null &&
                systemId.length() >= 36 &&
                !systemId.matches("[0-]*");
    }

    public static void checkAndLogContainer(final RuntimeInfo info) {
        Container container = info.getContainer();
        if (container == Container.SENTINEL || container == Container.OPENNMS) {
            log.info("We are running in {}", container);
        } else {
            log.warn("We are running in an unknown container, expect undetermined results! Container = {}", container);
        }
    }
}
