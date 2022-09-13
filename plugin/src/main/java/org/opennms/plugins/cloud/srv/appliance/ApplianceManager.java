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

import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.plugins.cloud.srv.appliance.cloud.api.entities.ListAppliances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ApplianceManager implements GrpcService {
    private static final Logger LOG = LoggerFactory.getLogger(ApplianceManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Map<String, ApplianceConfig> configMap = new HashMap<>();

    private static OkHttpClient httpClient = null;

    private static final String API_KEY_HEADER = "X-API-Key";

    // TECH-DEBT: The cloud API key should ultimately be retrieved in the initial handshaking process between
    //  this plugin and the Platform Access Service (PAS). Using a hard-coded, manually created one for now.
    private static final String CLOUD_API_KEY = "088a644f-d12c-4b71-8c6a-849986c6208a|PIkOk371KjwpV0GS";

    // TECH-DEBT: make this configurable within the cloud plugin.
    private static final String CLOUD_BASE_URL = "https://dev.cloud.opennms.com/api/v1/external";

    public ApplianceManager() {
        // TECH-DEBT: revise initialization (and shutdown) of this shared HTTP client.
        httpClient = new OkHttpClient();
    }

    @Override
    public void initGrpc(GrpcConnectionConfig grpcConfig) {
    }

    public void updateApplianceList() {
        // trigger query of portal appliance list API. parse results and add any new
        // UUIDs to our node table with appropriate metadata via requisition provider
    }

    public ListAppliances listAppliances() {
        var request = new Request.Builder()
                .get()
                .header(API_KEY_HEADER, CLOUD_API_KEY)
                .url(CLOUD_BASE_URL + "/appliance")
                .build();

        var call = httpClient.newCall(request);

        try (var response = call.execute()) {
            if (response.isSuccessful()) {
                if (response.body() == null) {
                    throw new IllegalStateException("Unable to list appliances from appliance service: Body is null");
                }

                var appliances =  MAPPER.readValue(response.body().bytes(), ListAppliances.class);
                LOG.info("Retrieved " + appliances.getTotalRecords() + " appliances.");
                return appliances;

            } else {
                throw new IllegalStateException("Unable to list appliances from appliance service:" +
                        " HTTP " + response.code() + " Message: " + response.message());
            }
        } catch (Exception e) {
            LOG.error("Unable to list appliances from the appliance service", e);
        }

        return null;
    }
}
