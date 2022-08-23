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

import org.opennms.plugins.cloud.config.ConfigurationManager;

public class CloudConfigRestServiceImpl implements CloudConfigRestService {

    private final ConfigurationManager cm;
    public CloudConfigRestServiceImpl(final ConfigurationManager cm) {
        this.cm = Objects.requireNonNull(cm);
    }

    @Override
    public Response putActivationKey(final String keyJson) {
        String key = extractKey(keyJson);
        this.cm.initConfiguration(key);
        this.cm.configure();
        return getStatus();
    }

    private String extractKey(final String json) {
        // TODO: Patrick: check why we get the key in such a strange format.
        // {"key":{"__v_isShallow":false,"dep":{"w":0,"n":0},"__v_isRef":true,"_rawValue":"aaa","_value":"aaa"}}
        String before = "\"_rawValue\":\"";
        String key = json.substring(json.indexOf(before) + before.length());
        return key.substring(0, key.indexOf("\""));
    }

    @Override
    public Response getStatus() {
        return Response
                .status(Response.Status.OK)
                .entity("{\"status\":\"" + cm.getStatus() + "\"}")
                .build();
    }
}
