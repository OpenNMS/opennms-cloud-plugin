package org.opennms.plugins.cloud.tsaas.shell;

import org.junit.Test;
import org.opennms.plugins.cloud.tsaas.shell.TestSonar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestSonarTest {

    @Test
    public void testTrueFunc (){
        String[] args = {"true"};
        assertTrue(TestSonar.func(args));
    }

    @Test
    public void testFalseFunc (){
        String[] args = {"false"};
        assertFalse(TestSonar.func(args));
    }

    @Test
    public void testFalseEmptyArg (){
        String[] args = {};
        assertFalse(TestSonar.func(args));
    }

    @Test
    public void testTrueFunc2 (){
        String[] args = {"true"};
        assertTrue(TestSonar.func2(args));
    }

    @Test
    public void testFalseFunc2 (){
        String[] args = {"false"};
        assertFalse(TestSonar.func2(args));
    }

}
