package org.opennms.plugins.cloud.srv.appliance.cloud.api.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplianceRecord {
    private String id;
    private String label;
    private ApplianceMinion minion;

    public ApplianceRecord() {
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

    public ApplianceMinion getMinion() {
        return minion;
    }

    public void setMinion(ApplianceMinion minion) {
        this.minion = minion;
    }

    @Override
    public String toString() {
        return "ApplianceRecord{" +
                "id='" + id + '\'' +
                ", label='" + label + '\'' +
                ", minion=" + minion +
                '}';
    }
}
