/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019-2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;

import org.opennms.integration.api.v1.config.requisition.Requisition;
import org.opennms.integration.api.v1.config.requisition.SnmpPrimaryType;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionInterface;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisition;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionMetaData;
import org.opennms.integration.api.v1.config.requisition.immutables.ImmutableRequisitionNode;
import org.opennms.integration.api.v1.requisition.RequisitionProvider;
import org.opennms.integration.api.v1.requisition.RequisitionRequest;

public class ApplianceRequisitionProvider implements RequisitionProvider {
    public static final String TYPE = "ApplianceRequisitionProvider";

    private final RequisitionTestContextManager requisitionManager;
    private final ApplianceManager applianceManager;

    public ApplianceRequisitionProvider(RequisitionTestContextManager requisitionManager, ApplianceManager applmgr) {
        this.requisitionManager = Objects.requireNonNull(requisitionManager);
        this.applianceManager = Objects.requireNonNull(applmgr);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public RequisitionRequest getRequest(Map<String, String> parameters) {
        return new ApplianceRequisitionRequest(parameters);
    }

    @Override
    public Requisition getRequisition(RequisitionRequest genericRequest) {
        final ApplianceRequisitionRequest request = (ApplianceRequisitionRequest)genericRequest;
        requisitionManager.trackGetRequisitionForSession(request.getSessionId());
        final InetAddress loopback = InetAddress.getLoopbackAddress();
        ImmutableRequisition.Builder builder = ImmutableRequisition.newBuilder();
        builder.setForeignSource(request.getForeignSource());

        // loop over appliance records
        Map<String,ApplianceConfig> appliances = applianceManager.getConfigMap();
        for(String key: appliances.keySet()) {
            ApplianceConfig cfg = appliances.get(key);
            ImmutableRequisitionNode.Builder nodeBuilder = ImmutableRequisitionNode.newBuilder();
            nodeBuilder
                    .setForeignId(cfg.getName())
                    .setNodeLabel(cfg.getName())
                    .addMetaData(ImmutableRequisitionMetaData.newBuilder()
                            .setContext("appliance")
                            .setKey("cloudUUID")
                            .setValue(cfg.getUuid())
                            .build());
            for(String address: cfg.getAddresses()) {
                if(address.indexOf(':') >= 0)
                    continue;
                
                try {
                    nodeBuilder.addInterface(ImmutableRequisitionInterface.newBuilder()
                            .setIpAddress(InetAddress.getByName(address))
                            .setSnmpPrimary(SnmpPrimaryType.PRIMARY)
                            .build());
                } catch(UnknownHostException e) {
                }
            }

            builder.addNode(nodeBuilder.build())
                    .build();
        }
        return builder.build();
    }

    @Override
    public byte[] marshalRequest(RequisitionRequest request) {
        throw new UnsupportedOperationException("No Minion support.");
    }

    @Override
    public RequisitionRequest unmarshalRequest(byte[] bytes) {
        throw new UnsupportedOperationException("No Minion support.");
    }

    private static class ApplianceRequisitionRequest implements RequisitionRequest {
        private final Map<String, String> parameters;

        public ApplianceRequisitionRequest(Map<String, String> parameters) {
            this.parameters = Objects.requireNonNull(parameters);
        }

        public String getForeignSource() {
            return parameters.getOrDefault("foreignSource", "Appliances");
        }

        public String getSessionId() {
            return parameters.get(RequisitionTestContextManager.SESSION_ID_PARM_NAME);
        }
    }
}
