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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.events.EventForwarder;
import org.opennms.integration.api.v1.model.InMemoryEvent;
import org.opennms.integration.api.v1.model.Node;
import org.opennms.integration.api.v1.model.immutables.ImmutableEventParameter;
import org.opennms.integration.api.v1.model.immutables.ImmutableInMemoryEvent;
import org.opennms.plugins.cloud.srv.appliance.cloud.api.entities.ApplianceRecord;
import org.opennms.plugins.cloud.srv.appliance.cloud.api.entities.GetApplianceInfoResponse;
import org.opennms.plugins.cloud.srv.appliance.cloud.api.entities.GetApplianceStatesResponse;
import org.opennms.plugins.cloud.srv.appliance.cloud.api.entities.GetLocationResponse;
import org.opennms.plugins.cloud.srv.appliance.cloud.api.entities.ListAppliancesResponse;
import org.opennms.plugins.cloud.srv.appliance.portal.api.entities.BrokerType;
import org.opennms.plugins.cloud.srv.appliance.portal.api.entities.ConnectivityProfile;
import org.opennms.plugins.cloud.srv.appliance.portal.api.entities.IdentityRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ApplianceManager {
    private static final Logger LOG = LoggerFactory.getLogger(ApplianceManager.class);
    private final NodeDao nodeDao;
    private final EventForwarder eventForwarder;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Map<String, ApplianceConfig> getConfigMap() {
        return configMap;
    }

    private final Map<String, ApplianceConfig> configMap = new HashMap<>();

    // This needs to be closed on shutdown
    private CloseableHttpClient httpclient;

    private static final String API_KEY_HEADER = "X-API-Key";

    // TECH-DEBT: The cloud API key should ultimately be retrieved in the initial handshaking process between
    //  this plugin and the Platform Access Service (PAS). Using a hard-coded, manually created one for now.
    private static final String CLOUD_API_KEY = "088a644f-d12c-4b71-8c6a-849986c6208a|PIkOk371KjwpV0GS";

    // TECH-DEBT: make this configurable within the cloud plugin.
    private static final String CLOUD_BASE_URL = "https://dev.cloud.opennms.com/api/v1/external";

    // TODO: fill this in...
    private static final String PAS_TOKEN = "";

    private static final String PORTAL_BASE_URL = "https://dev.cloud.opennms.com/api/portal";

    public ApplianceManager(NodeDao dao, EventForwarder ef) {
        this.eventForwarder = ef;
        this.nodeDao = dao;
        httpclient = HttpClients.createDefault();
    }

    public void updateApplianceList() {
        // trigger query of portal appliance list API. parse results and add any new
        // UUIDs to our node table with appropriate metadata via requisition provider

        var appliances = listAppliances();
        if (appliances.isEmpty()) {
            LOG.warn("Appliances list from cloud portal is empty");
            return;
        }

        appliances.forEach(appliance -> {
            var applianceConfig = new ApplianceConfig();
            applianceConfig.setUuid(appliance.getId());
            applianceConfig.setName(appliance.getLabel());
            GetApplianceInfoResponse resp = getApplianceInfo(appliance.getId());
            resp.getIpInfo().values().forEach(element -> {
                element.forEach(cidr -> {
                    int slashpos = cidr.indexOf('/');
                    String address = cidr.substring(0, slashpos);
                    applianceConfig.addAddress(address);
                });
            });
            configMap.put(appliance.getId(), applianceConfig);
        });

        LOG.info("Loaded config map with " + appliances.size() + " entries.");

        RequisitionTestContextManager requisitionManager = new RequisitionTestContextManager();
        try (RequisitionTestContextManager.RequisitionTestSession testSession = requisitionManager.newSession()) {
            final String foreignSource = "Appliances-" + testSession.getSessionId();

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

    // TECH-DEBT: only the first page will be read - pagination is not fully supported.
    public List<ApplianceRecord> listAppliances() {
        var request = new HttpGet(CLOUD_BASE_URL + "/appliance");
        request.addHeader(API_KEY_HEADER, CLOUD_API_KEY);

        try (var response = httpclient.execute(request)) {
            var statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                var appliances = MAPPER.readValue(response.getEntity().getContent(), ListAppliancesResponse.class);
                LOG.info("Retrieved " + appliances.getTotalRecords() + " appliances.");
                return appliances.getPagedRecords();
            } else {
                throw new IllegalStateException("Unable to list appliances from appliance service:" +
                        " HTTP " + statusCode + ".");
            }
        } catch (Exception e) {
            LOG.error("Unable to list appliances from the appliance service", e);
        }

        return Collections.emptyList();
    }

    // NOTE: if the appliance is offline, the cloud API responds with HTTP 400 - and this method will return null.
    public GetApplianceInfoResponse getApplianceInfo(String applianceId) {
        var request = new HttpGet(CLOUD_BASE_URL + "/appliance/" + applianceId + "/info");
        request.addHeader(API_KEY_HEADER, CLOUD_API_KEY);

        try (var response = httpclient.execute(request)) {
            var statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return MAPPER.readValue(response.getEntity().getContent(), GetApplianceInfoResponse.class);
            } else {
                throw new IllegalStateException("Unable to get appliance info from appliance service:" + " HTTP " + statusCode + ".");
            }
        } catch (Exception e) {
            LOG.error("Unable to get appliance info from the appliance service", e);
        }

        return null;
    }

    public GetApplianceStatesResponse getApplianceStates(String applianceId) {
        var request = new HttpGet(CLOUD_BASE_URL + "/appliance/" + applianceId + "/status");
        request.addHeader(API_KEY_HEADER, CLOUD_API_KEY);

        try (var response = httpclient.execute(request)) {
            var statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return MAPPER.readValue(response.getEntity().getContent(), GetApplianceStatesResponse.class);
            } else {
                throw new IllegalStateException("Unable to get appliance states from appliance service:" + " HTTP " + statusCode + ".");
            }
        } catch (Exception e) {
            LOG.error("Unable to get appliance states from the appliance service", e);
        }

        return null;
    }

    public void createConnectivityProfile(String instanceId, OnmsHttpInfo httpInfo, OnmsBrokerActiveMq broker) {
        var connectivity = new ConnectivityProfile();
        connectivity.setName("main");
        connectivity.setOnmsInstanceId(instanceId);
        connectivity.setBrokerType(BrokerType.JMS);
        connectivity.setHttpUrl(httpInfo.getHttpUrl());
        connectivity.setHttpUser(httpInfo.getHttpUser());
        connectivity.setHttpPassword(httpInfo.getHttpPassword());
        connectivity.setBrokerConfig(MAPPER.convertValue(broker, JsonNode.class));
        createConnectivityProfile(instanceId, connectivity);
    }

    public void createConnectivityProfile(String instanceId, OnmsHttpInfo httpInfo, OnmsBrokerKafka broker) {
        var connectivity = new ConnectivityProfile();
        connectivity.setName("main");
        connectivity.setOnmsInstanceId(instanceId);
        connectivity.setBrokerType(BrokerType.KAFKA);
        connectivity.setHttpUrl(httpInfo.getHttpUrl());
        connectivity.setHttpUser(httpInfo.getHttpUser());
        connectivity.setHttpPassword(httpInfo.getHttpPassword());
        connectivity.setBrokerConfig(MAPPER.convertValue(broker, JsonNode.class));
        createConnectivityProfile(instanceId, connectivity);
    }

    private void createConnectivityProfile(String instanceId, ConnectivityProfile profile) {
        var identify = new IdentityRequestEntity();
        identify.setInstanceId(instanceId);
        identify.setConnectivity(profile);

        StringEntity entity;
        try {
            entity = new StringEntity(MAPPER.writeValueAsString(identify));
        } catch (Exception e) {
            LOG.error("Unable to create connectivity profile: Unable to serialize request payload.", e);
            return;
        }

        var request = new HttpPost(PORTAL_BASE_URL + "/identify");
        request.addHeader("Authorization", "Bearer " + PAS_TOKEN);
        request.addHeader("Content-Type", "application/json");
        request.setEntity(entity);

        try (var response = httpclient.execute(request)) {
            var statusCode = response.getStatusLine().getStatusCode();
            if (!(statusCode >= 200 && statusCode < 300)) {
                 var responseBody = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))
                                        .lines()
                                        .collect(Collectors.joining("\n"));
                throw new IllegalStateException("Unable to create connectivity profile:" +
                        " HTTP " + statusCode + ": Body: " + responseBody);
            }
        } catch (Exception e) {
            LOG.error("Unable to create connectivity profile.", e);
        }
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

    public GetLocationResponse getLocationById(String locationId) {
        var request = new HttpGet(CLOUD_BASE_URL + "/location/" + locationId);
        request.addHeader(API_KEY_HEADER, CLOUD_API_KEY);

        try (var response = httpclient.execute(request)) {
            var statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return MAPPER.readValue(response.getEntity().getContent(), GetLocationResponse.class);
            } else {
                throw new IllegalStateException("Unable to get location " + locationId + " from appliance service:" +
                        " HTTP " + statusCode + ".");
            }
        } catch (Exception e) {
            LOG.error("Unable to get location " + locationId + " from appliance service.", e);
        }

        return null;
    }

    private void createLocation() {
    }

    private void updateAppliance(String applianceId, String locationId) {
        // Call getAppliance()
        // Given the result, feed in provided locationId under 'minion'.
    }

    private void getAppliance(String applianceId) {
    }

}
