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

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Convenient way to collect closables and close them at once.
 * Handles Exceptions and null objects.
 */
@Slf4j
public class CloseUtil implements AutoCloseable {
    final List<AutoCloseable> closeables = new ArrayList<>();

    public CloseUtil add(AutoCloseable closeable) {
        if (closeable != null) {
            closeables.add(closeable);
        }
        return this;
    }

    @Override
    public void close() {
        for (AutoCloseable closable : closeables) {
            close(closable);
        }
    }

    public static void close(final AutoCloseable closable) {
        try {
            if (closable != null) {
                closable.close();
            }
        } catch (Exception e) {
            log.warn("An exception occurred while trying to close", e);
        }
    }
}
