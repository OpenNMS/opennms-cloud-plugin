package org.opennms.plugins.cloud.srv.appliance;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OnmsBrokerKafka {

    @JsonProperty("bootstrap.servers")
    private String bootstrapServers;

    public OnmsBrokerKafka() {
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    @Override
    public String toString() {
        return "OnmsBrokerKafka{" +
                "bootstrapServers='" + bootstrapServers + '\'' +
                '}';
    }
}
