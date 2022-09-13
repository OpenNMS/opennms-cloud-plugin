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

import java.util.HashMap;
import java.util.Map;

import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplianceManager implements GrpcService {
    private static final Logger LOG = LoggerFactory.getLogger(ApplianceManager.class);
    private Map<String, ApplianceConfig> configMap = new HashMap<>();


    public ApplianceManager() {
    }

    @Override
    public void initGrpc(GrpcConnectionConfig grpcConfig) {
//        GrpcConnection<TimeseriesGrpc.TimeseriesBlockingStub> oldGrpc = this.grpc;
        LOG.debug("Initializing Grpc Connection with host {} and port {}", grpcConfig.getHost(), grpcConfig.getPort());
//        this.grpc = new GrpcConnection<>(grpcConfig, TimeseriesGrpc::newBlockingStub);
//        if(oldGrpc != null) {
//            try {
//                oldGrpc.shutDown();
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
//        }
    }

    public void updateApplianceList() {
        // trigger query of portal appliance list API. parse results and add any new
        // UUIDs to our node table with appropriate metadata via requisition provider
    }
}
