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

package org.opennms.plugins.cloud.ittest;

import java.io.IOException;

import org.opennms.plugins.cloud.testserver.MockCloud;

import lombok.extern.slf4j.Slf4j;

/**
 * Starts a mock server to simulate the serverside of the grpc gateway.
 * TsaaS is backed by an in memory storage.
 * Only for testing.
 * Open topics:
 * - Proper start / stop
 *
 */
@Slf4j
public class TestserverMain {

    public static final String MOCK_CLOUD_HOST = "horizon";
    public static final int MOCK_CLOUD_PORT = 9003;

    public static void main(String[] args) throws InterruptedException {
        try (MockCloud cloud = MockCloud.builder()
                .serverConfig(MockCloud.defaultServerConfig()
                        .host(MOCK_CLOUD_HOST) // we run the mock cloud directly in horizon
                        .port(MOCK_CLOUD_PORT)
                        .build())
                .certPrefix("/cert/horizon")
                .build()) {
            try {
                cloud.start();
            } catch (IOException e) {
                System.out.printf("MockCloud Server could not start on port %S%n", MOCK_CLOUD_PORT);
                log.error("MockCloud Server could not start on port {}", MOCK_CLOUD_PORT, e);
                System.exit(1); // not more we can do
            }
            System.out.printf("MockCloud Server started on port %s%n", MOCK_CLOUD_PORT);
            log.info("MockCloud Server started on port {}", MOCK_CLOUD_PORT);
            Thread.sleep(Long.MAX_VALUE); // wait till the end of time .
        }
    }
}
