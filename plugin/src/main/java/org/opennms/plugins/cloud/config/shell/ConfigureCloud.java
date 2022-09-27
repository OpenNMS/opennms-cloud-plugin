/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.plugins.cloud.config.shell;

import static org.opennms.integration.api.v1.runtime.Container.OPENNMS;
import static org.opennms.integration.api.v1.runtime.Container.SENTINEL;

import java.util.Objects;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.integration.api.v1.runtime.RuntimeInfo;
import org.opennms.plugins.cloud.config.ConfigurationManager;

@Command(scope = "opennms-cloud", name = "init",
        description = "Core: Contacts platform access service (PAS) and retrieves configuration\n" +
                "Sentinel: Contacts Core and retrieves configuration")


@Service
public class ConfigureCloud implements Action {

    @Reference
    private ConfigurationManager manager;

    @Reference
    private RuntimeInfo runtimeInfo;

    @Argument()
    String apiKey;

    @Override
    public Object execute() {
        if (OPENNMS.equals(this.runtimeInfo.getContainer())) {
            initForCore();
        } else if (SENTINEL.equals(this.runtimeInfo.getContainer())) {
            initForSentinel();
        } else {
            System.err.printf("It looks like we are running in a non supported environment, supported=OPENNMS, SENTINEL but it is %s",
                    this.runtimeInfo.getContainer());
        }
        return null;
    }

    public void initForCore() {
        if(apiKey == null || apiKey.isBlank()) {
            System.out.println("please enter api key");
        }
        Objects.requireNonNull(this.apiKey);
        manager.initConfiguration(apiKey);
        ConfigurationManager.ConfigStatus status = manager.configure();
        if(ConfigurationManager.ConfigStatus.CONFIGURED == status) {
            System.out.println("Initialization of Core was successful.");
        } else {
            System.out.printf("Initialization failed: %s. Check log (log:display) for details.", status);
        }
    }

    public void initForSentinel() {
        ConfigurationManager.ConfigStatus status = manager.configure();
        if(ConfigurationManager.ConfigStatus.CONFIGURED == status) {
            System.out.println("Initialization of Sentinel from Core was successful.");
        } else {
            System.out.printf("Initialization failed: %s. Check log (log:display) for details.", status);
        }
    }

}
