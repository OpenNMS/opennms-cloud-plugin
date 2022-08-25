package org.opennms.plugins.cloud.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrerequisiteChecker {

    private static final Logger LOG = LoggerFactory.getLogger(PrerequisiteChecker.class);

    static boolean isSystemIdOk(final String systemId) {
        if(systemId == null ||
                systemId.length() < 36 ||
                systemId.matches("[0-]*")){
            LOG.error("No unique system id: '{}'. Please check in table 'monitoringsystems'.", systemId);
            return false;
        }
        return true;
    }
}
