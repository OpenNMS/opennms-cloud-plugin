package org.opennms.plugins.cloud.config;


public class PrerequisiteChecker {

    private PrerequisiteChecker() {
        // Utility class
    }

    static boolean isSystemIdOk(final String systemId) {
        return systemId != null &&
                systemId.length() >= 36 &&
                !systemId.matches("[0-]*");
    }
}
