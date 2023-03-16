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

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.security.SecureRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import io.grpc.Metadata;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TraceParentHeaderGenerator {

    private static final int TRACE_ID_BYTE_LENGTH = 16;
    private static final int PARENT_ID_BYTE_LENGTH = 8;
    private static final String VERSION = "00";
    private static final String TRACE_FLAGS = "01";
    private static final String TRACEPARENT_KEY_NAME = "traceparent";

    public static TraceParentHeader generateTraceParentHeader() {
        return TraceParentHeader.builder()
                .version(VERSION)
                .traceId(encodeHexString(byteGenerator(TRACE_ID_BYTE_LENGTH)))
                .parentId(encodeHexString(byteGenerator(PARENT_ID_BYTE_LENGTH)))
                .traceFlags(TRACE_FLAGS)
                .build();
    }

    public static Metadata generateTraceParentHeaderMetadata() {
        return createMetadata(EMPTY);
    }

    public static Metadata generateTraceParentHeaderMetadata(String traceParentHeader) {
        return createMetadata(traceParentHeader);
    }

    private static Metadata createMetadata(String traceParentHeader) {
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of(TRACEPARENT_KEY_NAME, ASCII_STRING_MARSHALLER),
                StringUtils.isBlank(traceParentHeader) ? generateTraceParentHeader().createTraceParentHeaderAsString() :
                        traceParentHeader);

        Pattern p = Pattern.compile("00-(.*?)-(.*?)-0([01])");
        Matcher m = p.matcher(
                StringUtils.isBlank(traceParentHeader) ? generateTraceParentHeader().createTraceParentHeaderAsString() :
                        traceParentHeader);
        if (m.matches()) {
            metadata.put(Metadata.Key.of("x-b3-traceid", ASCII_STRING_MARSHALLER), m.group(1));
            metadata.put(Metadata.Key.of("x-b3-spanid", ASCII_STRING_MARSHALLER), m.group(2));
            metadata.put(Metadata.Key.of("x-b3-sampled", ASCII_STRING_MARSHALLER), m.group(3));
	}
        return metadata;
    }

    private static byte[] byteGenerator(int length) {
        SecureRandom random = new SecureRandom();
        byte[] r = new byte[length];
        random.nextBytes(r);
        return r;
    }
}
