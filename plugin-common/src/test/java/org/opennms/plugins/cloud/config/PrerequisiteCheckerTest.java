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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opennms.plugins.cloud.config.PrerequisiteChecker.isSystemIdOk;

import java.util.UUID;

import org.junit.Test;

public class PrerequisiteCheckerTest {

    @Test
    public void shouldCheckSystemId() {
        assertTrue(isSystemIdOk(UUID.randomUUID().toString()));
        assertFalse(isSystemIdOk(null));
        assertFalse(isSystemIdOk(""));
        assertFalse(isSystemIdOk(" "));
        assertFalse(isSystemIdOk("not 36 char long"));
        assertFalse(isSystemIdOk("00000000-0000-0000-0000-000000000000")); // default OpenNMS
    }

}