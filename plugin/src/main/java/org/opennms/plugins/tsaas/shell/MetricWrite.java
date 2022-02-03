package org.opennms.plugins.tsaas.shell;

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
import org.opennms.integration.api.v1.timeseries.TimeSeriesStorage;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableMetric;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableSample;

@Command(scope = "opennms-tsaas", name = "write-samples", description = "Write samples.")
@Service
public class MetricWrite implements Action {

    @Reference
    TimeSeriesStorage tss;

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
        for(int i=count; i>0; i--) {
            samples.add(ImmutableSample.builder()
                    .metric(metric)
                    .value((double)random.nextInt(8000 - 3000) + 3000)
                    .time(Instant.now().minus(i*10, ChronoUnit.MINUTES)).build());
        }
        System.out.printf("Storing %s samples.\n", count);
        tss.store(samples);
        System.out.println("Done.");
        return null;
    }

}
