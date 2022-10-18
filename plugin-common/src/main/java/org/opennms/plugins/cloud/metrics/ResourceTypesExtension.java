package org.opennms.plugins.cloud.metrics;

import java.util.List;

import org.opennms.integration.api.v1.config.datacollection.ResourceType;
import org.opennms.integration.api.xml.ClassPathResourceTypesLoader;

public class ResourceTypesExtension implements org.opennms.integration.api.v1.config.datacollection.ResourceTypesExtension {

    private final ClassPathResourceTypesLoader classPathResourceTypesLoader =
            new ClassPathResourceTypesLoader(ResourceTypesExtension.class, "cloud-plugin-resource.xml");

    @Override
    public List<ResourceType> getResourceTypes() {
        return classPathResourceTypesLoader.getResourceTypes();
    }
}