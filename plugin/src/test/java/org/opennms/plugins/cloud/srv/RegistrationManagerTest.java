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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.plugins.cloud.srv.tsaas.TsaasStorage;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class RegistrationManagerTest {

    private BundleContext context;
    private TsaasStorage tsaas;
    private RegistrationManager manager;
    ServiceRegistration<TimeSeriesStorage> registration;

    @Before
    public void setUp() {
        context = mock(BundleContext.class);
        registration = mock(ServiceRegistration.class);
        tsaas = mock(TsaasStorage.class);
        when(context.registerService(eq(TimeSeriesStorage.class), eq(tsaas), any())).thenReturn(registration);
        manager = new RegistrationManager(context, tsaas); // registers tsaas

    }

    @Test
    public void shouldRegisterOnlyWhenNotRegistered() {
        verify(context, times(1)).registerService(eq(TimeSeriesStorage.class), eq(tsaas), any());
        reset(context);
        manager.register(RegistrationManager.Service.tsaas);
        verify(context, never()).registerService(eq(TimeSeriesStorage.class), eq(tsaas), any());
    }

    @Test
    public void shouldDeregisterOnlyWhenRegistered() {
        manager.deregister(RegistrationManager.Service.tsaas);
        verify(registration, times(1)).unregister();
        reset(registration);
        manager.deregister(RegistrationManager.Service.tsaas);
        verify(registration, never()).unregister(); // we already deregisterd => no more deregistering
    }

    @Test
    public void destroyShouldDeregisterAllServices() {
        manager.destroy();
        verify(registration, times(1)).unregister();
    }

}