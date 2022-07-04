package org.opennms.plugins.cloud.tsaas.testserver;

import org.junit.Test;
import org.opennms.plugins.cloud.tsaas.shell.TestSonar;
import static org.junit.Assert.assertTrue;

public class TestSonarTest {

    @Test
    public void testTrue (){
        String[] args = {"true"};
        assertTrue(TestSonar.func(args));
    }

}
