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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;

public class TraceIdGeneratorTest {

    @Test
    public void traceParentHeaderShouldBeCorrectlyFormed() throws DecoderException {

        // When
        TraceParentHeader traceParentHeader = TraceParentHeaderGenerator.generateTraceParentHeader();

        // Then
        assertNotEquals(EMPTY, traceParentHeader.createTraceParentHeaderAsString());
        assertEquals(3, traceParentHeader.createTraceParentHeaderAsString().codePoints().filter(st -> st == '-').count());
        assertEquals("00", traceParentHeader.getVersion());
        assertEquals("01", traceParentHeader.getTraceFlags());
        assertEquals(16, Hex.decodeHex(traceParentHeader.getTraceId()).length);
        assertEquals(8, Hex.decodeHex(traceParentHeader.getParentId()).length);
    }
}
