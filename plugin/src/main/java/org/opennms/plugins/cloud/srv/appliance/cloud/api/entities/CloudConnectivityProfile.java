package org.opennms.plugins.cloud.srv.appliance.cloud.api.entities;

public class CloudConnectivityProfile {
    private String id;
    private String name;
    private OnmsInstance onmsInstance;

    public CloudConnectivityProfile() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OnmsInstance getOnmsInstance() {
        return onmsInstance;
    }

    public void setOnmsInstance(OnmsInstance onmsInstance) {
        this.onmsInstance = onmsInstance;
    }

    @Override
    public String toString() {
        return "ConnectivityProfile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", onmsInstance=" + onmsInstance +
                '}';
    }
}
