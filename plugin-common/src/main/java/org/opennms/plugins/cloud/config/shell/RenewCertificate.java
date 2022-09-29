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

import java.security.cert.CertificateException;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.plugins.cloud.config.ConfigurationManager;

@Command(scope = "opennms-cloud", name = "renewcert", description = "Contacts platform access service (PAS) and retrieves new certificates")
@Service
public class RenewCertificate implements Action {

    @Reference
    private ConfigurationManager manager;

    @Override
    public Object execute() throws CertificateException {
        manager.renewCerts();
        manager.configure();
        ConfigurationManager.ConfigStatus status = manager.getStatus();

        if(ConfigurationManager.ConfigStatus.CONFIGURED == status) {
            System.out.println("Renewing of certificates was successful.");
        } else {
            System.out.printf("Renewing of certificates: %s. Check log (log:display) for details.", status);
        }
        return null;
    }

}
