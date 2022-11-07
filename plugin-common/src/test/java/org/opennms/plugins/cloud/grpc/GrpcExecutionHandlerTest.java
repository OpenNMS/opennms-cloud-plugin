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

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.opennms.integration.api.v1.timeseries.StorageException;
import org.opennms.plugins.cloud.grpc.GrpcExecutionHandler.GrpcCall;
import org.opennms.tsaas.TimeseriesGrpc;
import org.opennms.tsaas.Tsaas;

import com.google.protobuf.Empty;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class GrpcExecutionHandlerTest {

    private static final MethodDescriptor<Tsaas.Samples, Empty> METHOD = TimeseriesGrpc.getStoreMethod();

    private GrpcExecutionHandler grpcHandler;
    private GrpcLogEntryQueue grpcLogEntryQueue;

    @Before
    public void setUp() {
        this.grpcLogEntryQueue = mock(GrpcLogEntryQueue.class);
        this.grpcHandler = new GrpcExecutionHandler(grpcLogEntryQueue);
    }

    @Test
    public void shouldSwallowNonRecoverableExceptions() throws StorageException {

        // no exception should be thrown:
        grpcHandler.executeRpcCallVoid(
                GrpcCall.builder()
                        .callToExecute(() -> {
                            throw new StatusRuntimeException(Status.UNIMPLEMENTED);
                        })
                        .methodDescriptor(METHOD)
                        .build());
        verify(grpcLogEntryQueue, times(1))
                .insertElementInQueue(anyLong(), anyLong(), eq(METHOD), eq(Status.UNIMPLEMENTED.getCode()));
        clearInvocations(grpcLogEntryQueue);

        grpcHandler.executeRpcCall(GrpcExecutionHandler.GrpcCall.builder()
                .callToExecute(() -> {
                    throw new StatusRuntimeException(Status.UNIMPLEMENTED);
                })
                .mapper((s) -> s)
                .defaultFunction(() -> "")
                .methodDescriptor(METHOD)
                .build());
        verify(grpcLogEntryQueue, times(1))
                .insertElementInQueue(anyLong(), anyLong(), eq(METHOD), eq(Status.UNIMPLEMENTED.getCode()));
    }

    @Test
    public void shouldRethrowRecoverableExceptions() {
        ThrowingRunnable run = () -> grpcHandler
                .executeRpcCallVoid(
                        GrpcCall
                                .builder()
                                .callToExecute(() -> {
                                    throw new StatusRuntimeException(Status.UNAVAILABLE);
                                })
                                .methodDescriptor(TimeseriesGrpc.getStoreMethod()).build());
        assertThrows(StorageException.class, run);
        verify(grpcLogEntryQueue, times(1))
                .insertElementInQueue(anyLong(), anyLong(), eq(METHOD), eq(Status.UNAVAILABLE.getCode()));
        clearInvocations(grpcLogEntryQueue);

        run = () -> grpcHandler.executeRpcCall(GrpcExecutionHandler.GrpcCall.builder()
                .callToExecute(() -> {
                    throw new StatusRuntimeException(Status.UNAVAILABLE);
                })
                .mapper((s) -> s)
                .defaultFunction(() -> "")
                .methodDescriptor(TimeseriesGrpc.getStoreMethod())
                .build());
        assertThrows(StorageException.class, run);
        verify(grpcLogEntryQueue, times(1))
                .insertElementInQueue(anyLong(), anyLong(), eq(METHOD), eq(Status.UNAVAILABLE.getCode()));
    }

}
