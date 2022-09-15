package org.opennms.plugins.cloud.srv.appliance.cloud.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Appliance {
    private String id;
    private String label;
    private String type;
    private String geoLocationLabel;
    private Float latitude;
    private Float longitude;
    private String applianceProfileId;
    private ApplianceMinion minion;
    private String subscriptionId;

    public Appliance() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGeoLocationLabel() {
        return geoLocationLabel;
    }

    public void setGeoLocationLabel(String geoLocationLabel) {
        this.geoLocationLabel = geoLocationLabel;
    }

    public Float getLatitude() {
        return latitude;
    }

    public void setLatitude(Float latitude) {
        this.latitude = latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public void setLongitude(Float longitude) {
        this.longitude = longitude;
    }

    public String getApplianceProfileId() {
        return applianceProfileId;
    }

    public void setApplianceProfileId(String applianceProfileId) {
        this.applianceProfileId = applianceProfileId;
    }

    public ApplianceMinion getMinion() {
        return minion;
    }

    public void setMinion(ApplianceMinion minion) {
        this.minion = minion;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public String toString() {
        return "ApplianceRecord{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", type='" + type + '\'' +
                ", geoLocationLabel='" + geoLocationLabel + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", applianceProfileId='" + applianceProfileId + '\'' +
                ", minion=" + minion +
                ", subscriptionId='" + subscriptionId + '\'' +
                '}';
    }
}
