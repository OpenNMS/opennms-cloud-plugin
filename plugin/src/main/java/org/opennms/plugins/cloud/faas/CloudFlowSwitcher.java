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

import org.opennms.core.ipc.sink.api.MessageDispatcherFactory;
import org.opennms.netmgt.telemetry.api.TelemetryListenerManager;
import org.opennms.netmgt.telemetry.api.registry.TelemetryRegistry;
import org.opennms.plugins.cloud.config.CloudConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class CloudFlowSwitcher {
    private static final Logger LOG = LoggerFactory.getLogger(CloudFlowSwitcher.class);

    private CloudConfig cloudConfig;
    private TelemetryListenerManager manager;
    private TelemetryRegistry cloudTelemetryRegistry;
    private LocalDispatcherFactory localDispatcherFactory;
    private TelemetryRegistry originalTelemetryRegistry;
    private MessageDispatcherFactory originalFactory;

    public CloudFlowSwitcher(CloudConfig cloudConfig, TelemetryRegistry cloudTelemetryRegistry, LocalDispatcherFactory localDispatcherFactory){
        this.cloudConfig = Objects.requireNonNull(cloudConfig);
        this.cloudTelemetryRegistry = Objects.requireNonNull(cloudTelemetryRegistry);
        this.localDispatcherFactory = Objects.requireNonNull(localDispatcherFactory);
    }

    public void init(){
        if(!"minion".equals(cloudConfig.getSystemName())){
            LOG.info("SKIP cloud flow for {}.", cloudConfig.getSystemName());
            return;
        }

        if (manager == null) {
            LOG.info("FAIL to switch due to TelemetryListenerManager is null.");
            return;
        }

        LOG.info("Switch to cloud flow.");
        this.originalFactory = manager.getMessageDispatcherFactory();
        manager.setMessageDispatcherFactory(localDispatcherFactory);

//        originalTelemetryRegistry = manager.getTelemetryRegistry();
//        manager.setTelemetryRegistry(cloudTelemetryRegistry);


//        for(Listener listener: manager.getListeners()){
//            listener.
//        }
    }

    public void destroy(){
        LOG.info("Switch back to original flow.");
        if (manager != null && originalTelemetryRegistry != null) {
            manager.setTelemetryRegistry(originalTelemetryRegistry);
        }
        if (manager != null && originalFactory != null) {
            manager.setMessageDispatcherFactory(originalFactory);
        }
    }

    public synchronized void onBind(TelemetryListenerManager manager) {
        this.manager = Objects.requireNonNull(manager);
        init();
    }
}
