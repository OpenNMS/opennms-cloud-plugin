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

import com.google.common.annotations.VisibleForTesting;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.opennms.plugins.cloud.srv.GrpcService;
import org.opennms.plugins.cloud.srv.appliance.cloud.api.entities.GetApplianceInfoResponse;
import org.opennms.plugins.cloud.srv.appliance.cloud.api.entities.GetApplianceStatesResponse;
import org.opennms.plugins.cloud.srv.appliance.cloud.api.entities.ListAppliancesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ApplianceManager implements GrpcService {
    private static final Logger LOG = LoggerFactory.getLogger(ApplianceManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Map<String, ApplianceConfig> configMap = new HashMap<>();

    private final OkHttpClient httpClient;

    private static final String API_KEY_HEADER = "X-API-Key";

    // TECH-DEBT: The cloud API key should ultimately be retrieved in the initial handshaking process between
    //  this plugin and the Platform Access Service (PAS). Using a hard-coded, manually created one for now.
    private static final String CLOUD_API_KEY = "088a644f-d12c-4b71-8c6a-849986c6208a|PIkOk371KjwpV0GS";

    // TECH-DEBT: make this configurable within the cloud plugin.
    private static final String CLOUD_BASE_URL = "https://dev.cloud.opennms.com/api/v1/external";

    public ApplianceManager() {
        httpClient = new OkHttpClient();
    }

    @VisibleForTesting
    public ApplianceManager(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public void initGrpc(GrpcConnectionConfig grpcConfig) {
    }

    public void updateApplianceList() {
        // trigger query of portal appliance list API. parse results and add any new
        // UUIDs to our node table with appropriate metadata via requisition provider
    }

    // TECH-DEBT: only the first page will be read - pagination is not fully supported.
    public ListAppliancesResponse listAppliances() {
        var request = new Request.Builder()
                .get()
                .header(API_KEY_HEADER, CLOUD_API_KEY)
                .url(CLOUD_BASE_URL + "/appliance")
                .build();

        var call = httpClient.newCall(request);

        try (var response = call.execute()) {
            if (response.isSuccessful()) {
                if (response.body() == null) {
                    throw new IllegalStateException("Unable to list appliances from appliance service: " +
                            "Response body is null");
                }

                var appliances =  MAPPER.readValue(response.body().bytes(), ListAppliancesResponse.class);
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

    // NOTE: if the appliance is offline, the cloud API responds with HTTP 400 - and this method will return null.
    public GetApplianceInfoResponse getApplianceInfo(String applianceId) {
        var request = new Request.Builder()
                .get()
                .header(API_KEY_HEADER, CLOUD_API_KEY)
                .url(CLOUD_BASE_URL + "/appliance/" + applianceId + "/info")
                .build();

        var call = httpClient.newCall(request);

        try (var response = call.execute()) {
            if (response.isSuccessful()) {
                if (response.body() == null) {
                    throw new IllegalStateException("Unable to get appliance info from appliance service: " +
                            "Response body is null");
                }
                return  MAPPER.readValue(response.body().bytes(), GetApplianceInfoResponse.class);
            } else {
                throw new IllegalStateException("Unable to get appliance info from appliance service:" +
                        " HTTP " + response.code() + " Message: " + response.message());
            }
        } catch (Exception e) {
            LOG.error("Unable to get appliance info from the appliance service", e);
        }

        return null;
    }

    public GetApplianceStatesResponse getApplianceStates(String applianceId) {
        var request = new Request.Builder()
                .get()
                .header(API_KEY_HEADER, CLOUD_API_KEY)
                .url(CLOUD_BASE_URL + "/appliance/" + applianceId + "/status")
                .build();

        var call = httpClient.newCall(request);

        try (var response = call.execute()) {
            if (response.isSuccessful()) {
                if (response.body() == null) {
                    throw new IllegalStateException("Unable to get appliance states from appliance service: " +
                            "Response body is null");
                }
                return  MAPPER.readValue(response.body().bytes(), GetApplianceStatesResponse.class);
            } else {
                throw new IllegalStateException("Unable to get appliance states from appliance service:" +
                        " HTTP " + response.code() + " Message: " + response.message());
            }
        } catch (Exception e) {
            LOG.error("Unable to get appliance states from the appliance service", e);
        }

        return null;
    }


    // This will reach out the 'portal' BE to create the instance.
    // Parameter 'instanceId' is the ID of the OpenNMs instance the user created by visiting the cloud portal and has
    //  imparted to the plugin.
    public String createConnectivityProfile(String instanceId) {
        return null;
    }

    public void setApplianceLocation(String applianceId, String locationName, String instanceId, String connectivityProfileId) {
        // Call getOrCreateLocation() - Get or create the location: must provide instanceId and connectivityProfileId.
        //  TBD about the Feature Profile, likely won't bother with that for now.

        // Call updateAppliance() - intent is to stand up the minion - need the ID from the call to 'getOrCreateLocation()'
    }

    // This returns the ID of the location
    private String getOrCreateLocation(String instanceId, String connectivityProfileId, String locationName) {
        return null;
    }

    private void updateAppliance(String applianceId, String locationId) {
        // Call getAppliance()
        // Given the result, feed in provided locationId under 'minion'.
    }

    private void getAppliance(String applianceId) {
    }
}
