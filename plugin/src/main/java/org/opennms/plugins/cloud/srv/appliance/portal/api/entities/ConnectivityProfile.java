package org.opennms.plugins.cloud.srv.appliance.portal.api.entities;

import com.fasterxml.jackson.databind.JsonNode;

public class ConnectivityProfile {
    private String name;
    private String onmsInstanceId;
    private String httpUrl;
    private String httpUser;
    private String httpPassword;
    private BrokerType brokerType;
    private JsonNode brokerConfig;

    public ConnectivityProfile() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOnmsInstanceId() {
        return onmsInstanceId;
    }

    public void setOnmsInstanceId(String onmsInstanceId) {
        this.onmsInstanceId = onmsInstanceId;
    }

    public String getHttpUrl() {
        return httpUrl;
    }

    public void setHttpUrl(String httpUrl) {
        this.httpUrl = httpUrl;
    }

    public String getHttpUser() {
        return httpUser;
    }

    public void setHttpUser(String httpUser) {
        this.httpUser = httpUser;
    }

    public String getHttpPassword() {
        return httpPassword;
    }

    public void setHttpPassword(String httpPassword) {
        this.httpPassword = httpPassword;
    }

    public BrokerType getBrokerType() {
        return brokerType;
    }

    public void setBrokerType(BrokerType brokerType) {
        this.brokerType = brokerType;
    }

    public JsonNode getBrokerConfig() {
        return brokerConfig;
    }

    public void setBrokerConfig(JsonNode brokerConfig) {
        this.brokerConfig = brokerConfig;
    }

    @Override
    public String toString() {
        return "ConnectivityProfile{" +
                "name='" + name + '\'' +
                ", onmsInstanceId='" + onmsInstanceId + '\'' +
                ", httpUrl='" + httpUrl + '\'' +
                ", httpUser='" + httpUser + '\'' +
                ", httpPassword='" + httpPassword + '\'' +
                ", brokerType=" + brokerType +
                ", brokerConfig=" + brokerConfig +
                '}';
    }
}
