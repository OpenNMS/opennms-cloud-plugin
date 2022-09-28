package org.opennms.plugins.cloud.srv.tsaas.shell;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.opennms.integration.api.v1.timeseries.Metric;
import org.opennms.integration.api.v1.timeseries.TagMatcher;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableTagMatcher;
import org.opennms.plugins.cloud.srv.tsaas.TsaasStorage;

@Command(scope = "opennms-tsaas", name = "query-metrics", description = "Find metrics.", detailedDescription= "pairs")
@Service
public class MetricQuery implements Action {

    @Reference
    TsaasStorage tss;

    @Argument(multiValued=true)
    List<String> arguments = new LinkedList<>();

    @Override
    public Object execute() throws Exception {
        final List<TagMatcher> tagMatchers = toTagMatchers(arguments);
        System.out.println("Querying metrics for tags: " + tagMatchers);
        List<Metric> metrics = tss.findMetrics(tagMatchers);
        System.out.println("Metrics:");
        for (Metric metric : metrics) {
            System.out.println("\t" + metric);
        }
        if (metrics.isEmpty()) {
            System.out.println("(No results returned)");
        }
        return null;
    }

    public static List<TagMatcher> toTagMatchers(final Collection<String> s) {
        if (s.size() % 2 == 1) {
            throw new IllegalArgumentException("collection must have an even number of arguments");
        }
        final AtomicInteger counter = new AtomicInteger(0);
        return s.stream().collect(
                Collectors.groupingBy(el -> {
                    final int i = counter.getAndIncrement();
                    return (i % 2 == 0) ? i : i - 1;
                })).values().stream()
                .map(a -> ImmutableTagMatcher.builder().key(a.get(0)).value(a.size() == 2 ? a.get(1) : null).build())
                .collect(Collectors.toList());
    }
}
