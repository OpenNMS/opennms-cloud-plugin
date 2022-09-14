package org.opennms.plugins.cloud.srv.appliance.cloud.api.entities;

public class GetApplianceStatesResponse {
    private Boolean connected;
    private String minionStatus;
    private String onmsStatus;

    public GetApplianceStatesResponse() {
    }

    public Boolean getConnected() {
        return connected;
    }

    public void setConnected(Boolean connected) {
        this.connected = connected;
    }

    public String getMinionStatus() {
        return minionStatus;
    }

    public void setMinionStatus(String minionStatus) {
        this.minionStatus = minionStatus;
    }

    public String getOnmsStatus() {
        return onmsStatus;
    }

    public void setOnmsStatus(String onmsStatus) {
        this.onmsStatus = onmsStatus;
    }

    @Override
    public String toString() {
        return "ApplianceStates{" +
                "connected='" + connected + '\'' +
                ", minionStatus='" + minionStatus + '\'' +
                ", onmsStatus='" + onmsStatus + '\'' +
                '}';
    }
}
