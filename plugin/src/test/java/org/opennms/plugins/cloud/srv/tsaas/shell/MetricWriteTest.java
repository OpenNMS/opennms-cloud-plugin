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

package org.opennms.plugins.cloud.srv.tsaas.shell;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.opennms.plugins.cloud.srv.tsaas.TsaasStorage;

public class MetricWriteTest {

    @Test
    public void shouldWrite() throws Exception {
        TsaasStorage tss = mock(TsaasStorage.class);
        final MetricWrite write = new MetricWrite();
        write.tss = tss;
        write.count = 3;
        write.execute();
        verify(tss, times(1)).store(any());
    }

    @Test
    public void shouldRejectNegativeCount() throws Exception {
        final MetricWrite write = new MetricWrite();
        write.count = -1;
        assertThrows(IllegalArgumentException.class, write::execute);
    }

    @Test
    public void shouldRejectZeroCount() throws Exception {
        final MetricWrite write = new MetricWrite();
        write.count = 0;
        assertThrows(IllegalArgumentException.class, write::execute);
    }
}