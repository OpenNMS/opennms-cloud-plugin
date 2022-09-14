package org.opennms.plugins.cloud.srv.appliance.cloud.api.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetApplianceInfoResponse {
    private String hostname;
    private Map<String, List<String>> ipInfo = new HashMap<>();

    public GetApplianceInfoResponse() {
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Map<String, List<String>> getIpInfo() {
        return ipInfo;
    }

    public void setIpInfo(Map<String, List<String>> ipInfo) {
        this.ipInfo = ipInfo;
    }

    @Override
    public String toString() {
        return "GetApplianceInfoResponse{" +
                "hostname='" + hostname + '\'' +
                ", ipInfo=" + ipInfo +
                '}';
    }
}
