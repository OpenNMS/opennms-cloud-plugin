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

package org.opennms.plugins.cloud;

import java.util.Hashtable;
import java.util.Objects;

import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.tsaas.TsaasStorage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for registering cloud services at OpenNMS.
 * Currently we support tsaas.
 */
public class ServiceManager {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceManager.class);

    private final BundleContext context;

    final TsaasStorage tsaas;
    private ServiceRegistration<TimeSeriesStorage> tsaasRegistration;

    public ServiceManager(final BundleContext context,
                          final TsaasStorage tsaas) {
        this.context = Objects.requireNonNull(context);
        this.tsaas = Objects.requireNonNull(tsaas);
        registerTsaas(); // TODO: Patrick: this should be later conditional, depending if the client enabled tsaas
    }

    public void registerTsaas() {
        if (this.tsaasRegistration != null) {
            LOG.warn("Cannot register tsaas, its already registered.");
            return;
        }
        Hashtable<String,Object> properties = new Hashtable<>();
        properties.put("registration.export", "true");
        this.tsaasRegistration = context.registerService(TimeSeriesStorage.class, tsaas, properties);
        LOG.info("Registered tsaas with OpenNMS.");
    }

    public void deregisterTsaas() {
        if (this.tsaasRegistration == null) {
            LOG.warn("Cannot deregister tsaas, it's not registered.");
            return;
        }
        this.tsaasRegistration.unregister();
        this.tsaasRegistration = null;
        LOG.info("Deregistered tsaas with OpenNMS.");
    }

    // called via blueprint
    public void destroy() {
        deregisterTsaas();
    }
}
