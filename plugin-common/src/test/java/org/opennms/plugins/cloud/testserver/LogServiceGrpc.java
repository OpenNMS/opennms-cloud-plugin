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

package org.opennms.plugins.cloud.testserver;

import java.util.ArrayList;
import java.util.List;

import org.opennms.plugins.cloud.grpc.CloudLogService;
import org.opennms.tsaas.telemetry.GatewayGrpc;
import org.opennms.tsaas.telemetry.GatewayOuterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;
import lombok.Getter;

public class LogServiceGrpc extends GatewayGrpc.GatewayImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(LogServiceGrpc.class);

    @Getter
    private static final List<GatewayOuterClass.LatencyTrace> list = new ArrayList<>();

    @Override
    public void sendTraces(GatewayOuterClass.SendTracesRequest request, StreamObserver<GatewayOuterClass.SendTracesResponse> responseObserver) {
        list.addAll(request.getLatencyTracesList());
        LOG.info("Received log message. {}", request.getLatencyTracesList());
        GatewayOuterClass.SendTracesResponse sendTracesResponse = GatewayOuterClass.SendTracesResponse.newBuilder().build();
        responseObserver.onNext(sendTracesResponse);
        responseObserver.onCompleted();
    }
}
