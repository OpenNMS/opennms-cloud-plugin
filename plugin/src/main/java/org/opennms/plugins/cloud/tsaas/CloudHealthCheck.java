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

package org.opennms.plugins.cloud.tsaas;

import java.util.Objects;

import org.opennms.integration.api.v1.health.Context;
import org.opennms.integration.api.v1.health.HealthCheck;
import org.opennms.integration.api.v1.health.Response;
import org.opennms.integration.api.v1.health.Status;
import org.opennms.integration.api.v1.health.immutables.ImmutableResponse;
import org.opennms.tsaas.Tsaas;

/**
 * Exposes the health of the cloud service to OIA.
 * Call in karaf shell via opennms:health-check
 */
public class CloudHealthCheck implements HealthCheck {

    private final TsaasStorage cloud;

    public CloudHealthCheck(final TsaasStorage cloud) {
        this.cloud = Objects.requireNonNull(cloud);
    }

    @Override
    public String getDescription() {
        return "Cloud status";
    }

    @Override
    public Response perform(final Context context) throws Exception {
        Tsaas.CheckHealthResponse response = cloud.checkHealth();
        Status status = toStatus(response.getStatus());
        String message = String.format("Cloud status=%s", response.getStatus().name());
        return ImmutableResponse.newInstance(status, message);
    }

    private Status toStatus(Tsaas.CheckHealthResponse.ServingStatus cloudStatus) {
        if (cloudStatus == Tsaas.CheckHealthResponse.ServingStatus.SERVING) {
            return Status.Success;
        }
        return Status.Failure;
    }
}
