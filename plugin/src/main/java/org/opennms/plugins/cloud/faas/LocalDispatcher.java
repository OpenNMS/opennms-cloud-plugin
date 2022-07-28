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

package org.opennms.plugins.cloud.faas;

import org.opennms.core.concurrent.LogPreservingThreadFactory;
import org.opennms.core.ipc.sink.api.AsyncDispatcher;
import org.opennms.core.ipc.sink.api.AsyncPolicy;
import org.opennms.core.ipc.sink.api.DispatchQueue;
import org.opennms.core.ipc.sink.api.Message;
import org.opennms.core.ipc.sink.api.WriteFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * It just a very simple dispatcher store message into queue. Intend to be replaced with backpressure implementation.
 */
public class LocalDispatcher<S extends Message> implements AsyncDispatcher<S> {
    private static final Logger LOG = LoggerFactory.getLogger(LocalDispatcher.class);

    private DispatchQueue<S> queue;
    private AsyncPolicy asyncPolicy;
    private FlowMessageHandler flowMessageHandler;

    private ExecutorService executor;
    private final AtomicInteger activeDispatchers = new AtomicInteger(0);
    private AtomicBoolean close = new AtomicBoolean(false);

    public LocalDispatcher(DispatchQueue<S> queue, AsyncPolicy asyncPolicy, FlowMessageHandler flowMessageHandler) {
        this.queue = Objects.requireNonNull(queue);
        this.asyncPolicy = Objects.requireNonNull(asyncPolicy);
        this.flowMessageHandler = Objects.requireNonNull(flowMessageHandler);

        executor = Executors.newFixedThreadPool(asyncPolicy.getNumThreads(),
                new LogPreservingThreadFactory(LocalDispatcherFactory.NAME_PREFIX + "Sink.AsyncDispatcher." +
                        queue, Integer.MAX_VALUE));
        startDrainingQueue();
    }

    private void startDrainingQueue() {
        for (int i = 0; i < asyncPolicy.getNumThreads(); i++) {
            executor.execute(this::dispatchFromQueue);
        }
    }

    private void dispatchFromQueue() {
        while (true) {
            try {
                LOG.trace("Asking dispatch queue for the next entry...");
                Map.Entry<String, S> messageEntry = queue.dequeue();
                if (messageEntry == null) {
                    break;
                }
                LOG.trace("Received message entry from dispatch queue {}", messageEntry);
                activeDispatchers.incrementAndGet();
                flowMessageHandler.handle(messageEntry.getValue());
                LOG.trace("Successfully handling message {}", messageEntry);
                activeDispatchers.decrementAndGet();
            } catch (InterruptedException e) {
                LOG.warn("Encountered InterruptedException while taking from dispatch queue", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.warn("Encountered exception while taking from dispatch queue", e);
            }
        }
    }

    @Override
    public CompletableFuture<DispatchStatus> send(S message) {
        if (close.get()) {
            throw new RuntimeException("Dispatcher closed!");
        }
        CompletableFuture<DispatchStatus> sendFuture = new CompletableFuture<>();
        try {
            //String key = InetAddressUtils.str(message.getSource().getAddress()) + DateTimeFormatter.ISO_DATE_TIME.format(message.getReceivedAt().toInstant());

            queue.enqueue(message, String.valueOf(System.nanoTime()));
            sendFuture.complete(DispatchStatus.QUEUED);
        } catch (WriteFailedException e) {
            sendFuture.completeExceptionally(e);
        }
        return sendFuture;
    }

    @Override
    public int getQueueSize() {
        return queue.getSize();
    }


    @Override
    public void close() throws Exception {
        close.set(true);
        while (true) {
            if (this.getQueueSize() == 0) {
                return;
            }
            Thread.sleep(100L);
        }
    }
}
