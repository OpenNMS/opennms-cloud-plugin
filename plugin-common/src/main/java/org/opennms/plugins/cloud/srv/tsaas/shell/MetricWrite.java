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

package org.opennms.plugins.cloud.srv.tsaas.shell;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.integration.api.v1.timeseries.IntrinsicTagNames;
import org.opennms.integration.api.v1.timeseries.Metric;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableMetric;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableSample;
import org.opennms.plugins.cloud.srv.tsaas.TsaasStorage;

@Command(scope = "opennms-tsaas", name = "write-samples", description = "Write samples.")
@Service
@SuppressWarnings("java:S106") // System.out is used intentionally: we want to see it in the Karaf shell
public class MetricWrite implements Action {

    @Reference
    TsaasStorage tss;

    @Option(name = "count")
    int count = 1;

    @Override
    public Object execute() throws Exception {
        if (count < 1) {
            throw new IllegalArgumentException("Count must be > 0");
        }

        Random random = new Random(42);
        Metric metric = ImmutableMetric.builder()
                .intrinsicTag(IntrinsicTagNames.resourceId, "response:127.0.0.1:icmp")
                .intrinsicTag(IntrinsicTagNames.name, "icmp")
                .metaTag("_idx2", "(response:127.0.0.1:icmp,3)")
                .metaTag("mtype", "gauge")
                .metaTag("ICMP/127.0.0.1", "icmp")
                .metaTag("_idx0", "(response,3)")
                .metaTag("_idx1", "(response:127.0.0.1,3)")
                .metaTag("_idx2w", "(response:127.0.0.1,*)")
                .build();
        List<Sample> samples = new ArrayList<>();
        for(long l=count; l>0; l--) {
            samples.add(ImmutableSample.builder()
                    .metric(metric)
                    .value((double)random.nextInt(8000 - 3000) + 3000)
                    .time(Instant.now().minus(l*10, ChronoUnit.MINUTES)).build());
        }
        System.out.printf("Storing %s samples.%n", count);
        tss.store(samples);
        System.out.println("Done.");
        return null;
    }

}
