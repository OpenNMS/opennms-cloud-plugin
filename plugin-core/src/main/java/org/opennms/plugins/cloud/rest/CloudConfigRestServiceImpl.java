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

import java.util.Objects;

import javax.ws.rs.core.Response;

import org.json.JSONObject;
import org.opennms.plugins.cloud.config.ConfigurationManager;

public class CloudConfigRestServiceImpl implements CloudConfigRestService {

    private final ConfigurationManager cm;

    public CloudConfigRestServiceImpl(final ConfigurationManager cm) {
        this.cm = Objects.requireNonNull(cm);
    }

    @Override
    public Response putActivationKey(final String keyJson) {
        try {
            String key = extractKey(keyJson);
            this.cm.initConfiguration(key);
            this.cm.configure();
        } catch (Exception e) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(exceptionToJson(e))
                    .build();
        }
        return getStatus();
    }

    String extractKey(final String json) {
        return new JSONObject(json)
                .getJSONObject("key")
                .getString("_rawValue");
    }

    @Override
    public Response getStatus() {
        return Response
                .status(Response.Status.OK)
                .entity(new JSONObject()
                        .put("status", cm.getStatus().name())
                        .toString())
                .build();
    }

    String exceptionToJson(Exception e) {
        return new JSONObject()
                .put("status", cm.getStatus().name())
                .put("message", e.getMessage())
                .toString();
    }
}
