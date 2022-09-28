package org.opennms.plugins.cloud.config;


import org.opennms.integration.api.v1.runtime.Container;
import org.opennms.integration.api.v1.runtime.RuntimeInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PrerequisiteChecker {

    private PrerequisiteChecker() {
        // Utility class
    }

    public static void checkAndLogSystemId(final String systemId) {
        if (isSystemIdOk(systemId)) {
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

    public static void checkAndLogContainer(final RuntimeInfo info) {
        Container container = info.getContainer();
        if (container == Container.SENTINEL || container == Container.OPENNMS) {
            log.info("We are running in {}", container);
        } else {
            log.warn("We are running in an unknown container, expect undetermined results! Container = {}", container);
        }
    }
}
