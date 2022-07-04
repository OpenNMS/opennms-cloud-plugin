package org.opennms.plugins.cloud.tsaas.shell;

public class TestSonar {

    public static void main(String[] args) {
        if (args.length> 0 && "true".equals(args[0])){
            System.out.println("true");
        } else {
            System.out.println("false");
        }
    }
}
