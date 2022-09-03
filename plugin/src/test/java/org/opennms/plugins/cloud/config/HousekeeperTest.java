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

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import java.time.Duration;
import java.time.Instant;

import org.junit.After;
import org.junit.Test;

public class HousekeeperTest {

    private Housekeeper hk;

    @Test
    public void shouldRenewConfigForExpiredToken() throws InterruptedException {
        ConfigurationManager cm = mock(ConfigurationManager.class);
        hk = new Housekeeper(cm, 1);
        doReturn(Instant.now().plusSeconds(60*60)) // token valid
                .doReturn(Instant.now()) // token expired
                .when(cm).getTokenExpiration();
        hk.init();
        await()
                .during(Duration.ofMillis(800)) // no config should be called during ramp up time (1sec)
                .atMost(Duration.ofMillis(2200)) // config should have been called within 2 sec
                .until (() -> !mockingDetails(cm).getInvocations().isEmpty());
    }

    @After
    public void tearDown() {
        hk.destroy();
    }

}