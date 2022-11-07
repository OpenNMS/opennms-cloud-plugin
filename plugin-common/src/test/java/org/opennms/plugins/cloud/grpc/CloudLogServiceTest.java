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

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.integration.api.v1.timeseries.StorageException;

public class CloudLogServiceTest extends CloudLogServiceTestUtil {

    private CloudLogService cloudLogService;

    private GrpcLogEntryQueue grpcLogEntryQueue;

    private GrpcExecutionHandler grpcExecutionHandlerSpy;

    @Before
    public void setUp() {
        grpcLogEntryQueue = new GrpcLogEntryQueue();
        GrpcExecutionHandler grpcExecutionHandler = new GrpcExecutionHandler(grpcLogEntryQueue);
        grpcExecutionHandlerSpy = Mockito.spy(grpcExecutionHandler);
        cloudLogService = new CloudLogService(grpcLogEntryQueue, grpcExecutionHandlerSpy);
    }

    @Test
    public void cloud_log_service_must_send_all_log_entries_in_the_expected_amount_of_calls() throws StorageException {
        // Given
        fillOutLogEntryQueue(2500, grpcLogEntryQueue);
        Mockito.doNothing().when(grpcExecutionHandlerSpy).executeRpcCallVoid(any());

        // When
        cloudLogService.handleLogQueue();

        // Then
        assertTrue(grpcLogEntryQueue.isQueueEmpty());
        verify(grpcExecutionHandlerSpy, times(3)).executeRpcCallVoid(any());

        // When
        clearInvocations(grpcExecutionHandlerSpy);
        cloudLogService.handleLogQueue();

        // Then
        assertTrue(grpcLogEntryQueue.isQueueEmpty());
        verifyZeroInteractions(grpcExecutionHandlerSpy);
    }
}