package org.opennms.plugins.cloud.metrics;

import java.util.List;

import org.opennms.integration.api.v1.config.datacollection.graphs.PrefabGraph;
import org.opennms.integration.api.xml.ClassPathGraphPropertiesLoader;

public class GraphPropertiesExtension implements org.opennms.integration.api.v1.config.datacollection.graphs.GraphPropertiesExtension {

    private ClassPathGraphPropertiesLoader graphPropertiesLoader = new ClassPathGraphPropertiesLoader(GraphPropertiesExtension.class,
            "cloud-plugin-graph.properties");

    @Override
    public List<PrefabGraph> getPrefabGraphs() {
        return graphPropertiesLoader.getGraphProperties();
    }
}