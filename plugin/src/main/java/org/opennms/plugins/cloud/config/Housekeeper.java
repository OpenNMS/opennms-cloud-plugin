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
import static org.opennms.integration.api.v1.runtime.Container.OPENNMS;
import static org.opennms.integration.api.v1.runtime.Container.SENTINEL;
import static org.opennms.plugins.cloud.config.ConfigStore.Key.grpchost;
import static org.opennms.plugins.cloud.config.ConfigStore.Key.grpcport;
import static org.opennms.plugins.cloud.config.ConfigStore.Key.privatekey;
import static org.opennms.plugins.cloud.config.ConfigStore.Key.publickey;

import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opennms.integration.api.v1.runtime.RuntimeInfo;
import org.opennms.plugins.cloud.grpc.GrpcConnectionConfig;

import lombok.extern.slf4j.Slf4j;

/**
 * Schedules and executes needed tasks.
 */
@Slf4j
public class Housekeeper {

    private final ScheduledExecutorService executor;
    private final int intervalInSecondsForToken;
    private final int intervalInSecondsForCert;
    private final int intervalForSyncInSeconds;

    private final ConfigurationManager configurationManager;
    private final ConfigStore config;

    private final RuntimeInfo runtimeInfo;
    private GrpcConnectionConfig currentConfig;

    public Housekeeper(final ConfigurationManager configurationManager,
                       final ConfigStore config,
                       final RuntimeInfo runtimeInfo,
                       final int intervalInSecondsForToken,
                       final int intervalInSecondsForCert,
                       final int intervalForSyncInSeconds) {
        this.configurationManager = configurationManager;
        this.config = Objects.requireNonNull(config);
        this.runtimeInfo = Objects.requireNonNull(runtimeInfo);
        executor = Executors.newSingleThreadScheduledExecutor();
        this.intervalInSecondsForToken = intervalInSecondsForToken;
        this.intervalInSecondsForCert = intervalInSecondsForCert;
        this.intervalForSyncInSeconds = intervalForSyncInSeconds;
    }

    public Housekeeper(final ConfigurationManager configurationManager,
                       final ConfigStore config,
                       final RuntimeInfo runtimeInfo) {
        this(configurationManager,
                config,
                runtimeInfo,
                60 * 60, // token check every 60 min
                60 * 50 * 24, // cert: check once per day
                60 * 5 // sync every 5 min
        );
    }


    public void init() {
        if (OPENNMS.equals(this.runtimeInfo.getContainer())) {
            initForOpenNMS();
        } else if (SENTINEL.equals(this.runtimeInfo.getContainer())) {
            initForSentinel();
        } else {
            log.error("It looks like we are running in a non supported environment, supported=OPENNMS, SENTINEL but it is {}",
                    this.runtimeInfo.getContainer());
        }

    }

    private void initForOpenNMS() {
        executor.scheduleAtFixedRate(() -> wrap(this::renewToken), 1, intervalInSecondsForToken, TimeUnit.SECONDS);
        executor.scheduleAtFixedRate(() -> wrap(this::renewCerts), 1, intervalInSecondsForCert, TimeUnit.SECONDS);
    }

    private void initForSentinel() {
        executor.scheduleAtFixedRate(() -> wrap(this::syncConfig), 1, intervalForSyncInSeconds, TimeUnit.SECONDS);
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

    public void syncConfig() {
        GrpcConnectionConfig newConfig = createConfig();
        if (!Objects.equals(this.currentConfig, newConfig)) {
            // config has changed => sync it
            configurationManager.configure();
            this.currentConfig = createConfig();
        }
    }

    private GrpcConnectionConfig createConfig() {
        // Creates a config object with all relevant fields that we want to monitor for change
        return GrpcConnectionConfig.builder()
                .privateKey(config.getOrNull(privatekey))
                .publicKey(config.getOrNull(publickey))
                .host(config.getOrNull(grpchost))
                .port(config.get(grpcport).map(Integer::valueOf).orElse(0))
                .build();
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
