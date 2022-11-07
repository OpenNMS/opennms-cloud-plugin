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

import static java.util.Objects.isNull;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.MethodDescriptor;
import io.grpc.Status;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class GrpcLogEntryQueue {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcLogEntryQueue.class);

    private ConcurrentLinkedQueue<LogEntry> logEntryQueue;

    public synchronized ConcurrentLinkedQueue<LogEntry> getlogEntryQueue() {
        if (isNull(logEntryQueue)) {
            logEntryQueue = new ConcurrentLinkedQueue<>();
        }
        return logEntryQueue;
    }

    public void insertElementInQueue(long startTime, long endTime, MethodDescriptor<?, ?> methodInvoked, Status.Code returnCode) {
        // TODO:  in DC-366: Plugin: Push latency data to cloud gateway
        LOG.debug("received cloud log with startTime={}, endTime={}, methodInvoked={}, returnCode={}",
                startTime, endTime, methodInvoked.getFullMethodName(), returnCode);

        getlogEntryQueue().add(LogEntry.builder()
                .startTime(startTime)
                .endTime(endTime)
                .methodInvoked(methodInvoked)
                .returnCode(returnCode).build());
    }

    public List<LogEntry> getQueueBatch(int batchSize) {
        return getlogEntryQueue().stream()
                .limit(batchSize).collect(Collectors.toList());
    }

    public void removeBatch(List<LogEntry> elementsToBeRemoved) {
        getlogEntryQueue().removeAll(elementsToBeRemoved);
        LOG.debug("Removed from log entry queue {} elements; current size: {}", elementsToBeRemoved.size(), this.logEntryQueue.size());
    }

    public boolean isQueueEmpty() {
        return getlogEntryQueue().isEmpty();
    }

    public boolean isQueueNotEmpty() {
        return !getlogEntryQueue().isEmpty();
    }
}