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

package org.opennms.plugins.cloud.config.shell;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.opennms.integration.api.v1.runtime.Container;
import org.opennms.integration.api.v1.runtime.RuntimeInfo;
import org.opennms.plugins.cloud.config.ConfigurationManager;

public class InitCloudTest {

    @Test
    public void shouldTalkToPasForCore() {
        RuntimeInfo info = mock(RuntimeInfo.class);
        when(info.getContainer()).thenReturn(Container.OPENNMS);
        ConfigurationManager manager = mock(ConfigurationManager.class);
        InitCloud cmd = new InitCloud();
        cmd.runtimeInfo = info;
        cmd.manager = manager;
        cmd.apiKey = "apiKey";
        cmd.execute();
        verify(manager, times(1)).initConfiguration(anyString());
        verify(manager, times(1)).configure();
    }

    @Test
    public void shouldTalkToDbForSentinel() {
        RuntimeInfo info = mock(RuntimeInfo.class);
        when(info.getContainer()).thenReturn(Container.SENTINEL);
        ConfigurationManager manager = mock(ConfigurationManager.class);
        InitCloud cmd = new InitCloud();
        cmd.runtimeInfo = info;
        cmd.manager = manager;
        cmd.execute();
        verify(manager, never()).initConfiguration(anyString());
        verify(manager, times(1)).configure();
    }
}