package org.opennms.plugins.cloud.tsaas.shell;

public class TestSonar {

    public static boolean func(String[] args) {
        if (args.length> 0 && "true".equals(args[0])){
            return true;
        } else {
            return false;
        }
    }
}
