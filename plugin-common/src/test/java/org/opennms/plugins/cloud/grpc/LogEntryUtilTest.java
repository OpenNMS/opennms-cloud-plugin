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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.opennms.plugins.cloud.grpc.LogEntryUtil.convertToLatencyTraceList;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.Test;
import org.opennms.tsaas.telemetry.GatewayOuterClass;

import com.google.protobuf.Timestamp;

import io.grpc.Status;

public class LogEntryUtilTest extends CloudLogServiceTestUtil {

    @Test
    public void long_millis_must_be_correctly_converted_to_g_protobuf_timestamp() {
        // Given
        LocalDateTime localDateTime = LocalDateTime.of(2022, 11, 16, 12, 0);
        long millis = localDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli();

        // When
        Timestamp timestamp = LogEntryUtil.convertToTimestamp(millis);

        // Then
        assertEquals(MILLISECONDS.toSeconds(millis), timestamp.getSeconds());
    }

    @Test
    public void log_entry_must_be_correctly_converted_to_latency_trace() {
        // Given
        int batchSize = 2;
        GrpcLogEntryQueue grpcLogEntryQueue = new GrpcLogEntryQueue();
        fillOutLogEntryQueue(batchSize, grpcLogEntryQueue);

        // When
        List<GatewayOuterClass.LatencyTrace> latencyTraces = convertToLatencyTraceList(grpcLogEntryQueue.getQueueBatch(batchSize));

        // Then
        assertEquals(2, latencyTraces.size());
        latencyTraces.forEach(latencyTrace -> {
            assertEquals("test-method", latencyTrace.getTraceId());
            assertEquals(Status.Code.OK.value(), latencyTrace.getStatusCode());
        });
    }
}