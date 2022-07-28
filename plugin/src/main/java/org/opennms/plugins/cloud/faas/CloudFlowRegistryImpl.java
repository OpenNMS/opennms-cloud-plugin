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

import com.codahale.metrics.MetricRegistry;
import org.opennms.core.ipc.sink.api.AsyncDispatcher;
import org.opennms.netmgt.telemetry.api.adapter.Adapter;
import org.opennms.netmgt.telemetry.api.receiver.Connector;
import org.opennms.netmgt.telemetry.api.receiver.Listener;
import org.opennms.netmgt.telemetry.api.receiver.Parser;
import org.opennms.netmgt.telemetry.api.receiver.TelemetryMessage;
import org.opennms.netmgt.telemetry.api.registry.TelemetryRegistry;
import org.opennms.netmgt.telemetry.config.api.AdapterDefinition;
import org.opennms.netmgt.telemetry.config.api.ConnectorDefinition;
import org.opennms.netmgt.telemetry.config.api.ListenerDefinition;
import org.opennms.netmgt.telemetry.config.api.ParserDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * It is used to override minion dispatcher to FAAS dispatcher while still keep the existing dispatcher. It makes sure old registry still working after cloud-plugin uninstalled.
 */
public class CloudFlowRegistryImpl implements TelemetryRegistry {
    private static final Logger LOG = LoggerFactory.getLogger(CloudFlowRegistryImpl.class);

    private final Map<String, AsyncDispatcher<TelemetryMessage>> localDispatchers = new ConcurrentHashMap<>();

    private TelemetryRegistry telemetryRegistry;

    // temp for POC need to replace with the backpressure implementation
    private LocalDispatcherFactory localDispatchFactory;

    public CloudFlowRegistryImpl(TelemetryRegistry telemetryRegistry, LocalDispatcherFactory localDispatchFactory) {
        this.telemetryRegistry = Objects.requireNonNull(telemetryRegistry);
        this.localDispatchFactory = Objects.requireNonNull(localDispatchFactory);
    }

    @Override
    public Adapter getAdapter(AdapterDefinition adapterDefinition) {
        return telemetryRegistry.getAdapter(adapterDefinition);
    }

    @Override
    public Listener getListener(ListenerDefinition listenerDefinition) {
        return telemetryRegistry.getListener(listenerDefinition);
    }

    @Override
    public Connector getConnector(ConnectorDefinition connectorDefinition) {
        return telemetryRegistry.getConnector(connectorDefinition);
    }

    @Override
    public Parser getParser(ParserDefinition parserDefinition) {
        return telemetryRegistry.getParser(parserDefinition);
    }

    @Override
    public void registerDispatcher(String queueName, AsyncDispatcher<TelemetryMessage> dispatcher) {
        telemetryRegistry.registerDispatcher(queueName, dispatcher);

        AsyncDispatcher<TelemetryMessage> localDispatcher = localDispatchFactory.createDispatcher(queueName, null);
        localDispatchers.put(queueName, localDispatcher);
    }

    @Override
    public void clearDispatchers() {
        telemetryRegistry.clearDispatchers();
        localDispatchers.clear();
    }

    @Override
    public Collection<AsyncDispatcher<TelemetryMessage>> getDispatchers() {
        return localDispatchers.values();
    }

    @Override
    public AsyncDispatcher<TelemetryMessage> getDispatcher(String queueName) {
        return localDispatchers.computeIfAbsent(queueName, name -> {
            var existing = telemetryRegistry.getDispatcher(name);
            return (existing != null) ? localDispatchFactory.createDispatcher(name, null) : null;
        });
    }

    @Override
    public void removeDispatcher(String queueName) {
        telemetryRegistry.removeDispatcher(queueName);
        localDispatchers.remove(queueName);
    }

    @Override
    public MetricRegistry getMetricRegistry() {
        return telemetryRegistry.getMetricRegistry();
    }
}
