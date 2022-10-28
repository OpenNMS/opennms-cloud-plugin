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

package org.opennms.plugins.cloud.srv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.srv.tsaas.TsaasStorage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for registering cloud services at OpenNMS.
 * Currently we support tsaas.
 */
public class RegistrationManager {

    private static final Logger LOG = LoggerFactory.getLogger(RegistrationManager.class);

    public enum Service {
        TSAAS, FAAS;

        public static boolean contains(final String value) {
            return Arrays.stream(values())
                    .map(Enum::name)
                    .anyMatch(name -> name.equals(value));
        }
    }

    private final BundleContext context;

    final TsaasStorage tsaas;

    private final Map<Service, ServiceRegistration<?>> registrations = new EnumMap<>(Service.class);

    public RegistrationManager(final BundleContext context,
                               final TsaasStorage tsaas) {
        this.context = Objects.requireNonNull(context);
        this.tsaas = Objects.requireNonNull(tsaas);
    }

    public void register(final Service service) {
        if (Service.TSAAS == service) {
            register(Service.TSAAS, TimeSeriesStorage.class, tsaas);
        } else if (Service.FAAS == service) {
            throw new UnsupportedOperationException("please implement me");
        } else {
            throw new UnsupportedOperationException("please implement me");
        }
    }

    private  <T> void register(final Service service, final Class<T> interfaceToRegister, final T serviceToRegister) {
        if (this.registrations.containsKey(service)) {
            LOG.warn("Cannot register {}, its already registered.", service);
            return;
        }
        @SuppressWarnings("java:S1149") // Hashtable is needed since that is what the Osgi interface expects
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("registration.export", "true");
        ServiceRegistration<T> registration = context.registerService(interfaceToRegister, serviceToRegister, properties);
        this.registrations.put(service, registration);
        LOG.info("Registered {} ({}) with OpenNMS.", service, interfaceToRegister);
    }

    public void deregister(final Service service) {
        ServiceRegistration<?> registration = this.registrations.get(service);
        if (registration == null) {
            LOG.warn("Cannot deregister {}, it's not registered.", service);
            return;
        }
        registration.unregister();
        this.registrations.remove(service);
        LOG.info("Deregistered {} with OpenNMS.", service);
    }

    // called via blueprint
    public void destroy() {
        List<Service> servicesToDeRegister = new ArrayList<>(registrations.keySet());
        for (Service service : servicesToDeRegister) {
            deregister(service);
        }
    }
}
