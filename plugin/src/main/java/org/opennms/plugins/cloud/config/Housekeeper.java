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

package org.opennms.plugins.cloud.config;

import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/** Schedules and executes needed tasks. */
@Slf4j
public class Housekeeper {

    private final ScheduledExecutorService executor;
    private final int intervalInSeconds;
    private final ConfigurationManager configurationManager;

    public Housekeeper(final ConfigurationManager configurationManager, final int intervalInSeconds) {
        executor = Executors.newSingleThreadScheduledExecutor();
        this.intervalInSeconds = intervalInSeconds;
        this.configurationManager = configurationManager;
    }

    public Housekeeper(final ConfigurationManager configurationManager) {
        this(configurationManager, 60 * 5);
    }


    public void init() {
        executor.scheduleAtFixedRate(() -> wrap(this::renewToken), intervalInSeconds, intervalInSeconds, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(() -> wrap(this::renewCerts), intervalInSeconds, intervalInSeconds, TimeUnit.SECONDS);
    }
    public void destroy() {
        executor.shutdown();
    }

    public void renewToken() {
        final Instant expirationDate = configurationManager.getTokenExpiration();
        // renew 20 min before expiry
        if (expirationDate.minusSeconds(60L * 20L).isBefore(Instant.now())) {
            log.info("Triggering renewal of configuration, token will expire soon.");
            this.configurationManager.configure();
        }
    }

    public void renewCerts() throws CertificateException {
        final Instant expirationDate = configurationManager.getCertExpiration();
        // renew 20 min before expiry
        if(expirationDate.minusSeconds(60L * 20L).isBefore(Instant.now())) {
            log.info("Triggering renewal of certificates, will expire soon.");
            this.configurationManager.renewCerts();
            this.configurationManager.configure();
        }
    }

    private void wrap(RunnableWithException r) {
        try {
            r.run();
        } catch (Exception e) {
            log.error("Job failed.", e);
        }
    }

    private interface RunnableWithException {
        void run() throws Exception;
    }

}
