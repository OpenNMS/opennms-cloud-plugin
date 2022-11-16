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
import org.junit.Test;

public class GrpcLogEntryQueueTest extends CloudLogServiceTestUtil {


    private GrpcLogEntryQueue grpcLogEntryQueue;

    @Before
    public void setUp() {
        grpcLogEntryQueue = new GrpcLogEntryQueue();
    }

    @Test
    public void entry_is_correctly_inserted_in_queue() {
        // When
        fillOutLogEntryQueue(1, grpcLogEntryQueue);

        // Then
        assertEquals(1, grpcLogEntryQueue.getlogEntryQueue().size());
    }

    @Test
    public void entries_are_correctly_batched_from_queue() {
        // When
        fillOutLogEntryQueue(100, grpcLogEntryQueue);

        // Then
        assertEquals(100, grpcLogEntryQueue.getlogEntryQueue().size());

        // When
        List<LogEntry> logEntryList = grpcLogEntryQueue.getQueueBatch(35);
        grpcLogEntryQueue.removeBatch(logEntryList);

        // Then
        assertEquals(35, logEntryList.size());
        assertEquals(65, grpcLogEntryQueue.getlogEntryQueue().size());
        assertTrue(grpcLogEntryQueue.isQueueNotEmpty());
    }
}