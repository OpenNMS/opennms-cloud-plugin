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

package org.opennms.plugins.cloud.tsaas;

import static io.grpc.Status.Code.DEADLINE_EXCEEDED;
import static io.grpc.Status.Code.OK;
import static io.grpc.Status.Code.UNAVAILABLE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.opennms.integration.api.v1.timeseries.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;

public class GrpcExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GrpcExceptionHandler.class);
  private static final Set<Code> RECOVERABLE_EXCEPTIONS = new HashSet<>(Arrays.asList(DEADLINE_EXCEEDED, UNAVAILABLE));

  static <T, R> R executeRpcCall(Supplier<T> callToExecute, Function<T, R> mapper, Supplier<R> defaultFunction) throws StorageException {
    try {
      T result = callToExecute.get();
      return mapper.apply(result);
    } catch (StatusRuntimeException ex) {
      Status.Code status = ex.getStatus().getCode();
      if (OK == status) {
        // should not happen but just to be safe...
        return defaultFunction.get();
      } else if (RECOVERABLE_EXCEPTIONS.contains(status)) {
        // network errors => recoverable => propagate error so OpenNMS can try later again.
        throw new StorageException(String.format("Network problem %s", status), ex);
      } else {
        // all other errors: we can't fix them => log and forget...
        LOG.warn("An error happened during the RPC call: {}", status, ex);
        return defaultFunction.get();
      }
    }
  }

  static <T> void executeRpcCall(Supplier<T> callToExecute) throws StorageException {
    try {
      T result = callToExecute.get();
    } catch (StatusRuntimeException ex) {
      Status.Code status = ex.getStatus().getCode();
      if (OK == status) {
        // should not happen but just to be safe...
      } else if (RECOVERABLE_EXCEPTIONS.contains(status)) {
        // network errors => recoverable => propagate error so OpenNMS can try later again.
        throw new StorageException(String.format("Network problem %s", status), ex);
      } else {
        LOG.warn("An error happened during the RPC call: {}", status, ex);
      }
    }
  }
}
