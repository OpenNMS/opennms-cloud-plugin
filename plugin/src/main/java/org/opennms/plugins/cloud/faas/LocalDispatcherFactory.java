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

import org.codehaus.jackson.map.ObjectMapper;
import org.opennms.core.ipc.sink.api.AsyncDispatcher;
import org.opennms.core.ipc.sink.api.AsyncPolicy;
import org.opennms.core.ipc.sink.api.DispatchQueue;
import org.opennms.core.ipc.sink.api.DispatchQueueFactory;
import org.opennms.core.ipc.sink.api.Message;
import org.opennms.core.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.core.ipc.sink.api.SinkModule;
import org.opennms.core.ipc.sink.api.SyncDispatcher;
import org.opennms.netmgt.telemetry.api.receiver.TelemetryMessage;

import java.io.IOException;
import java.util.Objects;

public class LocalDispatcherFactory implements MessageDispatcherFactory {
    public static final String NAME_PREFIX = "Faas.";
    private ObjectMapper mapper = new ObjectMapper();

    private DispatchQueueFactory dispatchQueueFactory;
    private FlowMessageHandler flowMessageHandler;

    public LocalDispatcherFactory(DispatchQueueFactory dispatchQueueFactory, FlowMessageHandler flowMessageHandler) {
        this.dispatchQueueFactory = Objects.requireNonNull(dispatchQueueFactory);
        this.flowMessageHandler = Objects.requireNonNull(flowMessageHandler);
    }

    private AsyncPolicy getPolicy() {
        return new AsyncPolicy() {
            @Override
            public int getQueueSize() {
                return 0;
            }

            @Override
            public int getNumThreads() {
                return 10;
            }

            @Override
            public boolean isBlockWhenFull() {
                return false;
            }
        };
    }

    public <S extends Message, T extends Message> AsyncDispatcher<S> createDispatcher(String name, SinkModule<S, T> module) {
        AsyncPolicy policy = this.getPolicy();
        DispatchQueue<TelemetryMessage> queue = dispatchQueueFactory.getQueue(policy,
                NAME_PREFIX + name, message -> {
                    try {
                        return mapper.writeValueAsBytes(message);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, bytes -> {
                    try {
                        return mapper.readValue(bytes, TelemetryMessage.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        return new LocalDispatcher(queue, policy, flowMessageHandler);
    }

    @Override
    public <S extends Message, T extends Message> SyncDispatcher<S> createSyncDispatcher(SinkModule<S, T> module) {
        return new SyncDispatcher<S>() {

            @Override
            public void close() throws Exception {
                throw new RuntimeException("Closed");
            }

            @Override
            public void send(S message) {
                flowMessageHandler.handle(message);
            }
        };
    }

    @Override
    public <S extends Message, T extends Message> AsyncDispatcher<S> createAsyncDispatcher(SinkModule<S, T> module) {
        return this.createDispatcher(module.getId(), module);

    }
}