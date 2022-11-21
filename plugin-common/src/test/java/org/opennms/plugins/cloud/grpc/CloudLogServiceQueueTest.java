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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CloudLogServiceQueueTest implements CloudLogServiceTestUtil {


    private CloudLogService cloudLogService;

    @Before
    public void setUp() {
        cloudLogService = new CloudLogService(new CloudLogServiceConfig(1000, 60));
        cloudLogService.deleteAll();
    }

    @Test
    public void entryIsCorrectlyInsertedInQueue() {
        // When
        fillOutLogEntryQueueCloudLog(1, cloudLogService);

        // Then
        assertEquals(1, cloudLogService.getLogEntryQueueSize());
    }

    @Test
    public void entriesAreCorrectlyBatchedFromQueue() {
        // When
        cloudLogService.deleteAll();
        fillOutLogEntryQueueCloudLog(100, cloudLogService);

        // Then
        assertEquals(100, cloudLogService.getLogEntryQueueSize());

        // When
        List<LogEntry> logEntryList = cloudLogService.getQueueBatch(35);
        cloudLogService.removeBatch(logEntryList);

        // Then
        assertEquals(35, logEntryList.size());
        assertEquals(65, cloudLogService.getLogEntryQueueSize());
        assertTrue(cloudLogService.isQueueNotEmpty());

        // When
        cloudLogService.deleteAll();
        assertTrue(cloudLogService.isQueueEmpty());
    }
}