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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CloudLogService.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "org.w3c.dom.*" })
public class CloudLogServiceTest implements CloudLogServiceTestUtil {

    private CloudLogService cloudLogService;

    private CloudLogService cloudLogServiceMock;

    private GrpcExecutionHandler grpcExecutionHandlerMock;

    private CloudLogServiceConfig cloudLogServiceConfig;

    @Before
    public void setUp() {
        cloudLogServiceConfig = new CloudLogServiceConfig(1000, 60);
        cloudLogService = new CloudLogService(cloudLogServiceConfig);
        cloudLogServiceMock = PowerMockito.spy(cloudLogService);
        grpcExecutionHandlerMock = Mockito.mock(GrpcExecutionHandler.class);
    }

    @Test
    public void cloudLogServiceMustSendAllLogEntriesInTheExpectedAmountOfCalls() throws Exception {
        // Given
        whenNew(GrpcExecutionHandler.class).withAnyArguments().thenReturn(grpcExecutionHandlerMock);
        fillOutLogEntryQueueCloudLog(2500, cloudLogServiceMock);
        when(cloudLogServiceMock.getGrpc()).thenReturn(PowerMockito.mock(GrpcConnection.class));

        // When
        cloudLogServiceMock.handleLogQueue();

        // Then
        assertTrue(cloudLogServiceMock.isQueueEmpty());
        verify(grpcExecutionHandlerMock, Mockito.times(3)).executeRpcCallVoid(any());

        // When
        cloudLogServiceMock.handleLogQueue();

        // Then
        assertTrue(cloudLogServiceMock.isQueueEmpty());
        verifyNoMoreInteractions(grpcExecutionHandlerMock);
    }
}