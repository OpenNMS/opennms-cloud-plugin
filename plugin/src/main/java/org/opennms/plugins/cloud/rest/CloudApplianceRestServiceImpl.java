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

import java.util.List;
import java.util.Objects;

import javax.ws.rs.core.Response;

import org.opennms.plugins.cloud.config.ConfigurationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudApplianceRestServiceImpl implements CloudApplianceRestService {
    private static final Logger LOG = LoggerFactory.getLogger(CloudApplianceRestServiceImpl.class);

    // TODO: May not need this?
    private final ConfigurationManager cm;

    public CloudApplianceRestServiceImpl(final ConfigurationManager cm) {
        this.cm = Objects.requireNonNull(cm);
    }

    @Override
    public Response getApplianceList(
        Integer limit,
        Integer offset
    ) {
        // TODO: Query ApplianceDao
        // For now, some dummy data

        CloudApplianceDTO dto0 = new CloudApplianceDTO();
        dto0.applianceId = "ae4968c1-d03e-4c68-89ea-875afaf7409f";
        dto0.applianceLabel = "virtual-appliance-1";
        dto0.applianceType = "VIRTUAL";
        dto0.applianceProfileId = null;
        dto0.minionLocationId = "0969a7bb-c846-4131-946e-35590f8e1317";
        dto0.nodeId = 123;
        dto0.nodeLabel = "node123";
        dto0.nodeLocation = "Kanata_Office";
        dto0.nodeIpAddress = "192.168.3.1";
        dto0.nodeStatus = "UP";

        CloudApplianceDTO dto1 = new CloudApplianceDTO();
        dto1.applianceId = "61143ceb-113d-4665-a79f-efb64f6f5599";
        dto1.applianceLabel = "hw-appliance-2";
        dto1.applianceType = "HARDWARE";
        dto1.applianceProfileId = null;
        dto1.minionLocationId = "cba8be81-7ec6-446c-aab4-9dfd3889dbbf";
        dto1.nodeId = 456;
        dto1.nodeLabel = "node456";
        dto1.nodeLocation = "Kanata_Office";
        dto1.nodeIpAddress = "192.168.3.2";
        dto1.nodeStatus = "DOWN";

        // this one has not been configured
        CloudApplianceDTO dto2 = new CloudApplianceDTO();
        dto2.applianceId = "59434aac-2cc4-48df-b2ca-bc7d29640852";
        dto2.applianceLabel = "hw-appliance-3";
        dto2.applianceType = "HARDWARE";
        dto2.applianceProfileId = null;
        dto2.minionLocationId = "f9389e2a-4c82-4fb6-bd08-c14213f73d8d";
        dto2.nodeId = null;
        dto2.nodeLabel = null;
        dto2.nodeLocation = null;
        dto2.nodeIpAddress = null;
        dto2.nodeStatus = "DOWN";

        List<CloudApplianceDTO> dtos = List.of(dto0, dto1, dto2);

        return Response
            .status(Response.Status.OK)
            .entity(dtos)
            .build();
    }

    @Override
    public Response configureAppliances() {
        return Response
            .status(Response.Status.OK)
            .entity("{\"status\": \"success\"}")
            .build();
    }
}
