package org.opennms.plugins.cloud.srv.appliance.cloud.api.entities;

public class Location {
    private String id;
    private String name;
    private String onmsInstanceId;
    private String connectivityProfileId;
    private String minionFeatureProfileId;

    public Location() {
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

    public String getOnmsInstanceId() {
        return onmsInstanceId;
    }

    public void setOnmsInstanceId(String onmsInstanceId) {
        this.onmsInstanceId = onmsInstanceId;
    }

    public String getConnectivityProfileId() {
        return connectivityProfileId;
    }

    public void setConnectivityProfileId(String connectivityProfileId) {
        this.connectivityProfileId = connectivityProfileId;
    }

    public String getMinionFeatureProfileId() {
        return minionFeatureProfileId;
    }

    public void setMinionFeatureProfileId(String minionFeatureProfileId) {
        this.minionFeatureProfileId = minionFeatureProfileId;
    }

    @Override
    public String toString() {
        return "Location{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", onmsInstanceId='" + onmsInstanceId + '\'' +
                ", connectivityProfileId='" + connectivityProfileId + '\'' +
                ", minionFeatureProfileId='" + minionFeatureProfileId + '\'' +
                '}';
    }
}
