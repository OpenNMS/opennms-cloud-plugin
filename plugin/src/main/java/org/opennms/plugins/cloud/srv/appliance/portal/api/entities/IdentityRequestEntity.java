package org.opennms.plugins.cloud.srv.appliance.portal.api.entities;

public class IdentityRequestEntity {
    private String instanceId;
    private String type = "CORE";
    private String version;
    private ConnectivityProfile connectivity;

    public IdentityRequestEntity() {
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ConnectivityProfile getConnectivity() {
        return connectivity;
    }

    public void setConnectivity(ConnectivityProfile connectivity) {
        this.connectivity = connectivity;
    }

    @Override
    public String toString() {
        return "IdentityRequestEntity{" +
                "instanceId='" + instanceId + '\'' +
                ", type='" + type + '\'' +
                ", version='" + version + '\'' +
                ", connectivity=" + connectivity +
                '}';
    }
}
