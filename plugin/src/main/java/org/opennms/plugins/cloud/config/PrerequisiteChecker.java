package org.opennms.plugins.cloud.config;


import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrerequisiteChecker {

    private PrerequisiteChecker() {
        // Utility class
    }

    public static void checkAndLogSystemId(final String systemId) {
        if(isSystemIdOk(systemId)) {
            log.info("System id is set to {}", systemId);
        } else {
            log.warn("System id is not set up. It is advisable to set it up, see here: https://github.com/OpenNMS/opennms-cloud-plugin#system-id");
        }
    }

    static boolean isSystemIdOk(final String systemId) {
        return systemId != null &&
                systemId.length() >= 36 &&
                !systemId.matches("[0-]*");
    }
}
