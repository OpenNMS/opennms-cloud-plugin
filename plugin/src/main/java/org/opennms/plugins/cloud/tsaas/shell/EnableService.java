package org.opennms.plugins.cloud.tsaas.shell;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.plugins.cloud.ServiceManager;

@Command(scope = "opennms-cloud", name = "enableService", description = "Enables a Cloud Service (e.g. Tsaas)")
@Service
public class EnableService implements Action {

    @Reference
    private ServiceManager manager;

    @Argument()
    String service;

    @Override
    public Object execute() throws Exception {
        manager.registerTsaas();
        return null;
    }
}
