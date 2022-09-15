package org.opennms.plugins.cloud.srv.appliance.portal.api.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OnmsBrokerActiveMq {
    @JsonProperty("broker-url")
    private String url;

    @JsonProperty("broker-user")
    private String user;

    @JsonProperty("broker-password")
    private String password;

    public OnmsBrokerActiveMq() {
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "OnmsBrokerActiveMq{" +
                "url='" + url + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
