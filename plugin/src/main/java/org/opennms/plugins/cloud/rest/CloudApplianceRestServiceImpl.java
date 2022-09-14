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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.core.Response;

import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.plugins.cloud.config.ConfigurationManager;
import org.opennms.plugins.cloud.dao.ApplianceDao;
import org.opennms.plugins.cloud.dao.ApplianceDaoImpl;
import org.opennms.plugins.cloud.dao.CloudApplianceDTO;
import org.opennms.plugins.cloud.srv.appliance.ApplianceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudApplianceRestServiceImpl implements CloudApplianceRestService {
    private static final Logger LOG = LoggerFactory.getLogger(CloudApplianceRestServiceImpl.class);

    private final ApplianceManager applianceManager;

    // TODO: May not need this?
    private final ConfigurationManager cm;

    private final ApplianceDao applianceDao;

    private final NodeDao nodeDao;

    public CloudApplianceRestServiceImpl(final ConfigurationManager cm, final ApplianceManager am,
                                         final NodeDao nodeDao) {
        this.cm = Objects.requireNonNull(cm);
        this.applianceManager = Objects.requireNonNull(am);
        this.nodeDao = Objects.requireNonNull(nodeDao);

        this.applianceDao = new ApplianceDaoImpl(this.applianceManager, this.nodeDao);
    }

    @Override
    public Response getApplianceList(
        Integer limit,
        Integer offset
    ) {
        LOG.info("In CloudApplianceRestServiceImpl.getApplianceList");
        List<CloudApplianceDTO> dtos = new ArrayList<>();

        try {
            LOG.info("Calling applianceDao");
            dtos = applianceDao.findAll();
        } catch (Exception e) {
            LOG.error("Error getting applianceDao info: {}", e.getMessage(), e);

            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"status\": \"failed\"}")
                    .build();
        }

        LOG.info("Returning response with {} DTOs", dtos.size());

        return Response
            .status(Response.Status.OK)
            .entity(dtos)
            .build();
    }

    @Override
    public Response configureAppliances() {
        LOG.info("In CloudApplianceRestServiceImpl.configureAppliances");

        // TODO: Error handling
        try {
            applianceManager.updateApplianceList();
        } catch (Exception e) {
            LOG.error("configureAppliances, ApplianceManager call failed: {}", e.getMessage());

            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"status\": \"failed\"}")
                    .build();
        }

        LOG.info("configureAppliances, returning OK response");

        return Response
            .status(Response.Status.OK)
            .entity("{\"status\": \"success\"}")
            .build();
    }
}
