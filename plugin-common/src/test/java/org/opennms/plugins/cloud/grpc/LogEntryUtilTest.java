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
import static org.junit.Assert.assertNotNull;
import static org.opennms.plugins.cloud.grpc.CloudLogServiceUtil.convertToLatencyTraceList;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.opennms.tsaas.telemetry.GatewayOuterClass;

import com.google.protobuf.Timestamp;

import io.grpc.Status;

public class LogEntryUtilTest implements CloudLogServiceTestUtil {

    @Test
    public void longMillisMustBeCorrectlyConvertedToProtobufTimestamp() {
        // Given
        LocalDateTime localDateTime = LocalDateTime.of(2022, 11, 16, 12, 0);
        long millis = localDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli();

        // When
        Timestamp timestamp = CloudLogServiceUtil.convertToTimestamp(millis);

        // Then
        assertEquals(MILLISECONDS.toSeconds(millis), timestamp.getSeconds());
    }

    @Test
    public void logEntryMustBeCorrectlyConvertedToLatencyTrace() {
        // Given
        int batchSize = 2;
        CloudLogService cloudLogService = new CloudLogService(new CloudLogServiceConfig(1000, 60));
        fillOutLogEntryQueueCloudLog(batchSize, cloudLogService);

        // When
        List<GatewayOuterClass.LatencyTrace> latencyTraces = convertToLatencyTraceList(cloudLogService.getQueueBatch(batchSize));

        // Then
        assertEquals(2, latencyTraces.size());
        latencyTraces.forEach(latencyTrace -> {
            assertNotNull(latencyTrace.getTraceId());
            assertEquals(Status.Code.OK.value(), latencyTrace.getStatusCode().getNumber());
            assertEquals(StringUtils.EMPTY, latencyTrace.getStatusMessage());
        });
    }
}