package org.opennms.plugins.cloud.srv.appliance.cloud.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplianceMinion {
    private String locationId;

    public ApplianceMinion() {
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    @Override
    public String toString() {
        return "ApplianceMinion{" +
                "locationId='" + locationId + '\'' +
                '}';
    }
}
