package org.opennms.plugins.cloud.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opennms.plugins.cloud.config.PrerequisiteChecker.isSystemIdOk;

import java.util.UUID;

import org.junit.Test;

public class PrerequisiteCheckerTest {

    @Test
    public void shouldCheckSystemId() {
        assertTrue(isSystemIdOk(UUID.randomUUID().toString()));
        assertFalse(isSystemIdOk(null));
        assertFalse(isSystemIdOk(""));
        assertFalse(isSystemIdOk(" "));
        assertFalse(isSystemIdOk("not 36 char long"));
        assertFalse(isSystemIdOk("00000000-0000-0000-0000-000000000000")); // default OpenNMS
    }

}