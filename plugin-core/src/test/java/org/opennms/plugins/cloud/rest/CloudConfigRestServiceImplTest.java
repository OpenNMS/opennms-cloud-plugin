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

package org.opennms.plugins.cloud.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.CONFIGURED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.DEACTIVATED;
import static org.opennms.plugins.cloud.config.ConfigurationManager.ConfigStatus.FAILED;
import static org.opennms.plugins.cloud.rest.CloudConfigRestServiceImpl.MESSAGE_KEY;
import static org.opennms.plugins.cloud.rest.CloudConfigRestServiceImpl.STATUS_KEY;

import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.opennms.plugins.cloud.config.ConfigurationManager;

public class CloudConfigRestServiceImplTest {

    // not sure why we get the key in such a strange format.
    private static final String API_KEY_JSON =  "{\"key\":{\"__v_isShallow\":false,\"dep\":{\"w\":0,\"n\":0},\"__v_isRef\":true,\"_rawValue\":\"aaa\",\"_value\":\"aaa\"}}";

    private ConfigurationManager cm;

    @Before
    public void setUp() {
        cm = mock(ConfigurationManager.class);
    }

    @Test
    public void shouldReturnStatus() {
        when(cm.getStatus()).thenReturn(CONFIGURED);
        String jsonString = new CloudConfigRestServiceImpl(cm).exceptionToJson(new NullPointerException("myProblem"));
        JSONObject json = new JSONObject(jsonString);
        assertEquals(CONFIGURED.name(), json.get(STATUS_KEY));
        assertEquals("myProblem", json.get(MESSAGE_KEY));
    }

    @Test
    public void shouldExtractKeyFromJson() {
        assertEquals("aaa", new CloudConfigRestServiceImpl(cm).extractKey(API_KEY_JSON));
    }

    @Test
    public void shouldPutActivationKey() {
        when(cm.getStatus()).thenReturn(CONFIGURED);
        Response response = new CloudConfigRestServiceImpl(cm)
                .putActivationKey(API_KEY_JSON);
        assertEquals(200, response.getStatus());
        String entity = (String) response.getEntity();
        JSONObject json = new JSONObject(entity);
        assertEquals(CONFIGURED.name(), json.get(STATUS_KEY));
    }

    @Test
    public void shouldPutActivationKeyWithException() {
        doThrow(new NullPointerException("ohoh")).when(cm).initConfiguration(anyString());
        when(cm.getStatus()).thenReturn(FAILED);
        Response response = new CloudConfigRestServiceImpl(cm)
                .putActivationKey(API_KEY_JSON);
        assertEquals(500, response.getStatus());
        String entity = (String) response.getEntity();
        JSONObject json = new JSONObject(entity);
        assertEquals(FAILED.name(), json.get(STATUS_KEY));
        assertEquals("ohoh", json.get(MESSAGE_KEY));
    }

    @Test
    public void shouldPutDeactivateKey() {
        when(cm.getStatus()).thenReturn(CONFIGURED);
        doNothing().when(cm).deactivateKeyConfiguration();

        Response response = new CloudConfigRestServiceImpl(cm)
                .putDeactivateKey(API_KEY_JSON);
        assertEquals(200, response.getStatus());
        String entity = (String) response.getEntity();
        JSONObject json = new JSONObject(entity);
        verify(cm, times(1)).deactivateKeyConfiguration();

        assertTrue(json.has(STATUS_KEY));
    }

    @Test
    public void shouldPutDeactivateKeyWithException() {
        doThrow(new NullPointerException("failed_deactivate")).when(cm).initConfiguration(anyString());
        when(cm.getStatus()).thenReturn(FAILED);
        Response response = new CloudConfigRestServiceImpl(cm)
                .putDeactivateKey(API_KEY_JSON);
        //This temporarily comes back as 200 but should be 500
        assertEquals(500, response.getStatus());
        String entity = (String) response.getEntity();
        JSONObject json = new JSONObject(entity);
        assertEquals(FAILED.name(), json.get(STATUS_KEY));
        assertEquals("failed_deactivate", json.get(MESSAGE_KEY));
    }
}