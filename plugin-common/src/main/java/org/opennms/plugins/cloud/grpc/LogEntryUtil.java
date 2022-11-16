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

import java.util.List;
import java.util.stream.Collectors;

import org.opennms.tsaas.telemetry.GatewayOuterClass;

import com.google.protobuf.Timestamp;

public class LogEntryUtil {

    public static List<GatewayOuterClass.LatencyTrace> convertToLatencyTraceList(List<LogEntry> logEntryList) {
        return logEntryList.stream().map(logEntry -> GatewayOuterClass.LatencyTrace.newBuilder()
                .setStartTime(convertToTimestamp(logEntry.getStartTime()))
                .setEndTime(convertToTimestamp(logEntry.getEndTime()))
                .setTraceId(logEntry.getMethodInvoked().getFullMethodName())
                .setStatusCode(logEntry.getReturnCode().value())
                .build()).collect(Collectors.toList());
    }

    public static Timestamp convertToTimestamp(long time) {
        return Timestamp.newBuilder().setSeconds(time / 1000)
                .setNanos((int) ((time % 1000) * 1000000)).build();
    }
}