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

package org.opennms.plugins.cloud.srv.appliance;

import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.events.EventForwarder;
import org.opennms.integration.api.v1.model.InMemoryEvent;
import org.opennms.integration.api.v1.model.Node;
import org.opennms.integration.api.v1.model.immutables.ImmutableEventParameter;
import org.opennms.integration.api.v1.model.immutables.ImmutableInMemoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplianceManager {
    private static final Logger LOG = LoggerFactory.getLogger(ApplianceManager.class);
    private Map<String, ApplianceConfig> configMap = new HashMap<>();
    private final NodeDao nodeDao;
    private final EventForwarder eventForwarder;

    public ApplianceManager(NodeDao dao, EventForwarder ef) {
        this.eventForwarder = ef;
        this.nodeDao = dao;

        RequisitionTestContextManager requisitionManager = new RequisitionTestContextManager();
        try (RequisitionTestContextManager.RequisitionTestSession testSession = requisitionManager.newSession()) {
            final String foreignSource = "oia-test-requisition-" + testSession.getSessionId();

            // Verify that no nodes are currently present for the foreign source
            List<Node> nodes = nodeDao.getNodesInForeignSource(foreignSource);
            if (!nodes.isEmpty()) {
                return;
            }

            final String url = String.format("requisition://%s?foreignSource=%s&sessionId=%s", ApplianceRequisitionProvider.TYPE, foreignSource, testSession.getSessionId());
            try {
                // Import the requisition
                final InMemoryEvent reloadImport = ImmutableInMemoryEvent.newBuilder()
                        .setUei("uei.opennms.org/internal/importer/reloadImport")
                        .setSource(ApplianceRequisitionProvider.class.getCanonicalName())
                        .addParameter(ImmutableEventParameter.newInstance("url", url))
                        .build();
                eventForwarder.sendSync(reloadImport);
            } catch(Exception e) {

            }
        }
    }

    public void updateApplianceList() {
        // trigger query of portal appliance list API. parse results and add any new
        // UUIDs to our node table with appropriate metadata via requisition provider
    }
}
