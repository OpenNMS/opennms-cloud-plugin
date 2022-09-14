package org.opennms.plugins.cloud.srv.appliance.cloud.api.entities;

public class GetLocationResponse {
    private String id;
    private String name;
    private String onmsInstanceId;
    private String minionFeatureProfileId;
    private String connectivityProfileId;

    public GetLocationResponse() {
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

    public String getMinionFeatureProfileId() {
        return minionFeatureProfileId;
    }

    public void setMinionFeatureProfileId(String minionFeatureProfileId) {
        this.minionFeatureProfileId = minionFeatureProfileId;
    }

    public String getConnectivityProfileId() {
        return connectivityProfileId;
    }

    public void setConnectivityProfileId(String connectivityProfileId) {
        this.connectivityProfileId = connectivityProfileId;
    }

    @Override
    public String toString() {
        return "GetLocationResponse{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", onmsInstanceId='" + onmsInstanceId + '\'' +
                ", minionFeatureProfileId='" + minionFeatureProfileId + '\'' +
                ", connectivityProfileId='" + connectivityProfileId + '\'' +
                '}';
    }
}
