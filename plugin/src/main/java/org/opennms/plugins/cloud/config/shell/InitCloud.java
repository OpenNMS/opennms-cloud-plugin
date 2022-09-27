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
public class InitCloud implements Action {

    @Reference
    ConfigurationManager manager;

    @Reference
    RuntimeInfo runtimeInfo;

    @Argument()
    String apiKey;

    @Override
    public Object execute() {
        ConfigurationManager.ConfigStatus status;
        if (OPENNMS.equals(this.runtimeInfo.getContainer())) {
            status = initForCore();
        } else if (SENTINEL.equals(this.runtimeInfo.getContainer())) {
            status = manager.configure();
        } else {
            System.err.printf("It looks like we are running in a non supported environment, supported=OPENNMS, SENTINEL but it is %s",
                    this.runtimeInfo.getContainer());
            return null;
        }

        if(ConfigurationManager.ConfigStatus.CONFIGURED == status) {
            System.out.printf("Initialization of %s was successful.%n", this.runtimeInfo.getContainer());
        } else {
            System.out.printf("Initialization of %s failed: %s. Check log (log:display) for details.%n", this.runtimeInfo.getContainer(), status);
        }

        return null;
    }

    private ConfigurationManager.ConfigStatus initForCore() {
        if(apiKey == null || apiKey.isBlank()) {
            System.out.println("please enter api key");
        }
        Objects.requireNonNull(this.apiKey);
        manager.initConfiguration(apiKey);
        return manager.configure();
    }

}
