package org.opennms.plugins.cloud.srv.appliance.cloud.api.entities;

public class OnmsInstance {
    private String id;
    private String name;

    public OnmsInstance() {
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

    @Override
    public String toString() {
        return "OnmsInstance{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
