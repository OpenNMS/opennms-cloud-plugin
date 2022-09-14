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

package org.opennms.plugins.cloud.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.model.IpInterface;
import org.opennms.integration.api.v1.model.MetaData;
import org.opennms.integration.api.v1.model.Node;
import org.opennms.plugins.cloud.srv.appliance.ApplianceManager;
import org.opennms.plugins.cloud.srv.appliance.cloud.api.entities.GetApplianceStatesResponse;

public class ApplianceDaoImpl implements ApplianceDao {
    private static final String CLOUD_UUID_METADATA_CONTEXT = "appliance";
    private static final String CLOUD_UUID_METADATA_KEY = "cloudUUID";

    private final ApplianceManager applianceManager;
    private final NodeDao nodeDao;

    public ApplianceDaoImpl(final ApplianceManager am, final NodeDao nodeDao) {
        this.applianceManager = am;
        this.nodeDao = nodeDao;
    }

    public List<CloudApplianceDTO> findAll() {
        List<CloudApplianceDTO> dtos = new ArrayList<>();

        List<Node> nodes = nodeDao.getNodes();

        List<Node> applianceNodes = nodes.stream().filter(ApplianceDaoImpl::isApplianceNode).collect(Collectors.toList());

        if (!applianceNodes.isEmpty()) {
            applianceNodes.forEach(n -> {
                CloudApplianceDTO dto = dtoFromNode(n);
                updateApplianceStatus(dto);
                dtos.add(dto);
            });
        }

        return dtos;
    }

    private CloudApplianceDTO dtoFromNode(Node node) {
        CloudApplianceDTO dto = new CloudApplianceDTO();

        dto.nodeId = node.getId();
        dto.nodeLabel = node.getLabel();
        dto.nodeLocation = node.getLocation();
        List<IpInterface> ipInterfaces = node.getIpInterfaces();

        if (!ipInterfaces.isEmpty()) {
            dto.nodeIpAddress = ipInterfaces.get(0).getIpAddress().getHostAddress();
        }

        // Enrich with appliance metadata
        // TODO: metadata context?
        dto.applianceCloudId = getCloudUuid(node);

        dto.nodeStatus = "UNKNOWN";

        return dto;
    }

    private void updateApplianceStatus(CloudApplianceDTO dto) {
        try {
            GetApplianceStatesResponse response = applianceManager.getApplianceStates(dto.applianceCloudId);

            if (response != null) {
                dto.hasStatus = true;
                dto.isConnected = response.getConnected();
                dto.minionStatus = response.getMinionStatus();
                dto.onmsStatus = response.getOnmsStatus();

                // TODO: Unsure which status to use, for now just using "connected"
                dto.nodeStatus = dto.isConnected ? "UP" : "DOWN";
            }
        } catch (Exception e) {
            // do nothing
        }
    }

    private static boolean isApplianceNode(Node node) {
        return node.getMetaData().stream()
            .anyMatch(m -> m.getKey().equals(CLOUD_UUID_METADATA_KEY));
    }

    private static String getCloudUuid(Node node) {
        return node.getMetaData().stream()
            .filter(m -> m.getContext() != null && m.getContext().equals(CLOUD_UUID_METADATA_CONTEXT)
                    && m.getKey() != null && m.getKey().equals(CLOUD_UUID_METADATA_KEY))
            .map(MetaData::getValue)
            .findFirst()
            .orElse("");
    }
}
