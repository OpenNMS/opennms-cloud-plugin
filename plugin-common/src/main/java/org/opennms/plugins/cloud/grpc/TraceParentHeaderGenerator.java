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

import static org.apache.commons.codec.binary.Hex.encodeHexString;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class TraceParentHeaderGenerator {

    private static final int TRACE_ID_BYTE_LENGTH = 16;
    private static final int PARENT_ID_BYTE_LENGTH = 8;
    private static final String VERSION = "00";
    private static final String TRACE_FLAGS = "01";


    public static TraceParentHeader generateTraceParentHeader() {
        return TraceParentHeader.builder()
                .version(VERSION)
                .traceId(encodeHexString(byteGenerator(TRACE_ID_BYTE_LENGTH)))
                .parentId(encodeHexString(byteGenerator(PARENT_ID_BYTE_LENGTH)))
                .traceFlags(TRACE_FLAGS)
                .build();
    }

    private static byte[] byteGenerator(int length) {
        Random random = ThreadLocalRandom.current();
        byte[] r = new byte[length];
        random.nextBytes(r);
        return r;
    }
}
