package org.opennms.plugins.cloud.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrerequisiteChecker {

    private static final Logger LOG = LoggerFactory.getLogger(PrerequisiteChecker.class);

    private PrerequisiteChecker() {
        // Utility class
    }

    static boolean isSystemIdOk(final String systemId) {
        return systemId != null &&
                systemId.length() >= 36 &&
                !systemId.matches("[0-]*");
    }
}
