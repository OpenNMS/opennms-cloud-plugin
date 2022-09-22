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

import static java.time.temporal.ChronoUnit.DAYS;

import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * Schedules and executes needed tasks.
 */
@Slf4j
public class Housekeeper {

    private final ScheduledExecutorService executor;
    private final int intervalInSecondsForToken;
    private final int intervalInSecondsForCert;

    private final ConfigurationManager configurationManager;

    public Housekeeper(final ConfigurationManager configurationManager,
                       final int intervalInSecondsForToken,
                       final int intervalInSecondsForCert) {
        this.configurationManager = configurationManager;
        executor = Executors.newSingleThreadScheduledExecutor();
        this.intervalInSecondsForToken = intervalInSecondsForToken;
        this.intervalInSecondsForCert = intervalInSecondsForCert;
    }

    public Housekeeper(final ConfigurationManager configurationManager) {
        this(configurationManager,
                60 * 60, // token echeck every 60 min
                60 * 50 * 24 // cert: check once per day
        );
    }


    public void init() {
        executor.scheduleAtFixedRate(() -> wrap(this::renewToken), 0, intervalInSecondsForToken, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(() -> wrap(this::renewCerts), 0, intervalInSecondsForCert, TimeUnit.SECONDS);
    }

    public void destroy() {
        executor.shutdown();
    }

    public void renewToken() {
        final Instant expirationDate = configurationManager.getTokenExpiration();
        // renew 24h before expiry
        if (expirationDate.minusSeconds(60L * 60L * 24L).isBefore(Instant.now())) {
            log.info("Triggering renewal of configuration, token will expire soon.");
            this.configurationManager.configure();
        }
    }

    public void renewCerts() throws CertificateException {
        final Instant expirationDate = configurationManager.getCertExpiration();
        // renew 7 days before expiry
        if (expirationDate.minus(7, DAYS).isBefore(Instant.now())) {
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
