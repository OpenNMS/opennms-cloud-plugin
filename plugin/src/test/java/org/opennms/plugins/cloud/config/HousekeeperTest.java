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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
        doReturn(Instant.now()
                .plusSeconds(60*60)) // first time: token valid
                .doReturn(Instant.now()) // second time: token expired
                .when(cm).getTokenExpiration();
        hk.init();
        verify(cm, times(0)).configure();
        await()
                .during(Duration.ofMillis(800)) // no config should be called during ramp up time (1sec)
                .atMost(Duration.ofMillis(5000)) // config should have been called by now
                .until (() -> mockingDetails(cm).getInvocations().stream().anyMatch(i -> "configure".equals(i.getMethod().getName())));
        verify(cm, times(1)).configure();
    }

    @After
    public void tearDown() {
        hk.destroy();
    }

}