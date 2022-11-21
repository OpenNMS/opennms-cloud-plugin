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

import static io.grpc.Status.Code.DEADLINE_EXCEEDED;
import static io.grpc.Status.Code.OK;
import static io.grpc.Status.Code.RESOURCE_EXHAUSTED;
import static io.grpc.Status.Code.UNAUTHENTICATED;
import static io.grpc.Status.Code.UNAVAILABLE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.opennms.integration.api.v1.timeseries.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import lombok.Builder;
import lombok.Data;

/**
 * Manages functionality for GRPC calls:
 * - ExceptionHandling: Tries to distinguish between
 * - recoverable Exceptions: will be propgated to OpenNMS and
 * - non-recoverable Exceptions: will be logged dropped
 * - Request logging (logs are send towards the cloud)
 * see also: https://www.grpc.io/docs/guides/error/
 */
public class GrpcExecutionHandler {

    private final CloudLogService cloudLogService;
    private static final Logger LOG = LoggerFactory.getLogger(GrpcExecutionHandler.class);
    private static final Set<Code> RECOVERABLE_EXCEPTIONS = new HashSet<>(Arrays.asList(
            DEADLINE_EXCEEDED,
            UNAVAILABLE,
            UNAUTHENTICATED,
            RESOURCE_EXHAUSTED));

    public GrpcExecutionHandler(CloudLogService cloudLogService) {
        this.cloudLogService = Objects.requireNonNull(cloudLogService);
    }

    public <T, R> R executeRpcCall(GrpcCall<T, R> callToExecute) throws StorageException {
        Objects.requireNonNull(callToExecute);
        Objects.requireNonNull(callToExecute.getMethodDescriptor());
        Objects.requireNonNull(callToExecute.getMapper());
        Objects.requireNonNull(callToExecute.getDefaultFunction());
        Status.Code status = OK;
        long startTime = System.currentTimeMillis();
        try {
            T result = callToExecute.getCallToExecute().get();
            return callToExecute.getMapper().apply(result);
        } catch (StatusRuntimeException ex) {
            status = ex.getStatus().getCode();
            if (OK == status) {
                // should not happen but just to be safe...
                return callToExecute.getDefaultFunction().get();
            } else if (RECOVERABLE_EXCEPTIONS.contains(status)) {
                // network errors => recoverable => propagate error so OpenNMS can try later again.
                throw new StorageException(String.format("Network problem %s", status), ex);
            } else {
                // all other errors: we can't fix them => log and forget...
                LOG.warn("An error happened during the RPC call: {}", status, ex);
                return callToExecute.getDefaultFunction().get();
            }
        } finally {
            cloudLogService.log(startTime, System.currentTimeMillis(), callToExecute.getMethodDescriptor(), status);
        }
    }

    public <T, R> void executeRpcCallVoid(GrpcCall<T, R> callToExecute) throws StorageException {
        Objects.requireNonNull(callToExecute);
        GrpcCall<T, R> call = callToExecute
                .toBuilder()
                .defaultFunction(() -> null) // set something so executeRpcCall() will work
                .mapper(t -> null) // set something so executeRpcCall() will work
                .build();
        executeRpcCall(call);
    }


    @Builder(toBuilder = true)
    @Data
    public static class GrpcCall<T, R> {
        final Supplier<T> callToExecute;
        final Function<T, R> mapper;
        final Supplier<R> defaultFunction;
        final MethodDescriptor<?, ?> methodDescriptor;
    }
}
