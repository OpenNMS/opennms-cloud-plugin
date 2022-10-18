package org.opennms.plugins.cloud.metrics;

import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.opennms.integration.api.v1.collectors.CollectionSet;
import org.opennms.integration.api.v1.collectors.CollectionSetPersistenceService;
import org.opennms.integration.api.v1.collectors.immutables.ImmutableNumericAttribute;
import org.opennms.integration.api.v1.collectors.resource.GenericTypeResource;
import org.opennms.integration.api.v1.collectors.resource.NodeResource;
import org.opennms.integration.api.v1.collectors.resource.NumericAttribute;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSet;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableCollectionSetResource;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableGenericTypeResource;
import org.opennms.integration.api.v1.collectors.resource.immutables.ImmutableNodeResource;
import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.model.Node;
import org.opennms.integration.api.v1.runtime.RuntimeInfo;
import org.opennms.plugins.cloud.srv.tsaas.TsaasStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricPusher {
    private static final Logger LOG = LoggerFactory.getLogger(MetricPusher.class);

    private static final long METRIC_PUSH_INTERVAL_MS = TimeUnit.SECONDS.toMillis(30);

    private final RuntimeInfo runtimeInfo;
    private final TsaasStorage storage;
    private final NodeDao nodeDao;
    private final CollectionSetPersistenceService collectionSetPersistenceService;

    private Timer timer;

    public MetricPusher(TsaasStorage storage, RuntimeInfo runtimeInfo, NodeDao nodeDao, CollectionSetPersistenceService collectionSetPersistenceService) {
        this.runtimeInfo = Objects.requireNonNull(runtimeInfo);
        this.storage = Objects.requireNonNull(storage);
        this.nodeDao = Objects.requireNonNull(nodeDao);
        this.collectionSetPersistenceService = Objects.requireNonNull(collectionSetPersistenceService);
    }

    public void init() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Thread.currentThread().setName("Cloud-MetricPusher");
                try {
                    gatherAndPersistMetrics();
                } catch (Exception e) {
                    LOG.error("Oops", e);
                }
            }
        }, TimeUnit.SECONDS.toMillis(5), METRIC_PUSH_INTERVAL_MS);
    }

    public void destroy() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void gatherAndPersistMetrics() {
        LOG.error("MOO: Gathering and persisting metrics for plugin!");
        Optional<Node> maybeNode = nodeDao.getNodesInForeignSource("selfmonitor").stream()
                .filter(n -> "Default".equals(n.getLocation()))
                .filter(n -> "localhost".equals(n.getLabel()))
                .findFirst();
        if (!maybeNode.isPresent()) {
            LOG.error("MOO: No node found.");
          return;
        }
        Node node = maybeNode.get();

        final ImmutableCollectionSet.Builder csetBuilder = ImmutableCollectionSet.newBuilder()
                .setStatus(CollectionSet.Status.SUCCEEDED);

        final NodeResource nodeResource = ImmutableNodeResource.newBuilder()
                .setNodeId(node.getId())
                .build();

        final GenericTypeResource cloudPluginResource = ImmutableGenericTypeResource.newBuilder()
                .setNodeResource(nodeResource)
                // FIXME: Include and sanitize system id
                .setInstance(runtimeInfo.getContainer().toString())
                .setType("cloudPlugin")
                .build();

        final ImmutableCollectionSetResource.Builder<?> resourceBuilder = ImmutableCollectionSetResource.newBuilder(GenericTypeResource.class)
                .setResource(cloudPluginResource);

        resourceBuilder.addNumericAttribute(counter("SamplesStored", "cloud-plugin-stats", () -> storage.getSamplesStoredMeter().getCount()));

        // Percentiles
        resourceBuilder.addNumericAttribute(gauge("StoreSamples75", "cloud-plugin-stats", () -> storage.getStoreSamplesTimer()
                .getSnapshot().get75thPercentile()));
        resourceBuilder.addNumericAttribute(gauge("StoreSamples95", "cloud-plugin-stats", () -> storage.getStoreSamplesTimer()
                .getSnapshot().get95thPercentile()));
        resourceBuilder.addNumericAttribute(gauge("StoreSamples98", "cloud-plugin-stats", () -> storage.getStoreSamplesTimer()
                .getSnapshot().get98thPercentile()));
        resourceBuilder.addNumericAttribute(gauge("StoreSamples99", "cloud-plugin-stats", () -> storage.getStoreSamplesTimer()
                .getSnapshot().get99thPercentile()));
        resourceBuilder.addNumericAttribute(gauge("StoreSamples999", "cloud-plugin-stats", () -> storage.getStoreSamplesTimer()
                .getSnapshot().get999thPercentile()));

        // Percentiles
        resourceBuilder.addNumericAttribute(gauge("GetSeries75", "cloud-plugin-stats", () -> storage.getGetTimeseriesTimer()
                .getSnapshot().get75thPercentile()));
        resourceBuilder.addNumericAttribute(gauge("GetSeries95", "cloud-plugin-stats", () -> storage.getGetTimeseriesTimer()
                .getSnapshot().get95thPercentile()));
        resourceBuilder.addNumericAttribute(gauge("GetSeries98", "cloud-plugin-stats", () -> storage.getGetTimeseriesTimer()
                .getSnapshot().get98thPercentile()));
        resourceBuilder.addNumericAttribute(gauge("GetSeries99", "cloud-plugin-stats", () -> storage.getGetTimeseriesTimer()
                .getSnapshot().get99thPercentile()));
        resourceBuilder.addNumericAttribute(gauge("GetSeries999", "cloud-plugin-stats", () -> storage.getGetTimeseriesTimer()
                .getSnapshot().get999thPercentile()));

        csetBuilder.addCollectionSetResource(resourceBuilder.build());

        collectionSetPersistenceService.persist(node.getId(), getFirstInetAddress(node), csetBuilder.build());
        LOG.error("MOO: Successfully pushed collection set node: {}: {}", node.getLabel(), csetBuilder.build());
    }

    private InetAddress getFirstInetAddress(Node onmsNode) {
        return onmsNode.getIpInterfaces().get(0).getIpAddress();
    }

    private NumericAttribute counter(String name, String group, Supplier<Long> value) {
        return ImmutableNumericAttribute.newBuilder()
                .setName(name)
                .setGroup(group)
                .setType(NumericAttribute.Type.COUNTER)
                .setValue(value.get().doubleValue())
                .build();
    }

    private NumericAttribute gauge(String name, String group, Supplier<Double> value) {
        return ImmutableNumericAttribute.newBuilder()
                .setName(name)
                .setGroup(group)
                .setType(NumericAttribute.Type.GAUGE)
                .setValue((double)TimeUnit.MILLISECONDS.convert(value.get().longValue(), TimeUnit.NANOSECONDS))
                .build();
    }

}