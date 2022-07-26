package org.opennms.plugins.cloud.tsaas.grpc;

import static org.opennms.tsaas.Tsaas.Aggregation.MAX;
import static org.opennms.tsaas.Tsaas.Aggregation.MIN;
import static org.opennms.tsaas.Tsaas.Aggregation.NONE;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.opennms.integration.api.v1.timeseries.Aggregation;
import org.opennms.integration.api.v1.timeseries.DataPoint;
import org.opennms.integration.api.v1.timeseries.IntrinsicTagNames;
import org.opennms.integration.api.v1.timeseries.MetaTagNames;
import org.opennms.integration.api.v1.timeseries.Metric;
import org.opennms.integration.api.v1.timeseries.Sample;
import org.opennms.integration.api.v1.timeseries.Tag;
import org.opennms.integration.api.v1.timeseries.TagMatcher;
import org.opennms.integration.api.v1.timeseries.TimeSeriesFetchRequest;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableMetric;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableSample;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableTag;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableTagMatcher;
import org.opennms.integration.api.v1.timeseries.immutables.ImmutableTimeSeriesFetchRequest;
import org.opennms.tsaas.Tsaas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Timestamp;

public class GrpcObjectMapper {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcObjectMapper.class);

    public static Tsaas.Tag toTag(Tag tag) {
        return Tsaas.Tag.newBuilder()
                .setKey(tag.getKey())
                .setValue(tag.getValue())
                .build();
    }

    public static Tag toTag(Tsaas.Tag tag) {
        return new ImmutableTag(tag.getKey(), tag.getValue());
    }

    public static Tsaas.TagMatcher toTagMatcher(TagMatcher tagMatcher) {
        return Tsaas.TagMatcher.newBuilder()
                .setType(toTagMatcherType(tagMatcher.getType()))
                .setKey(tagMatcher.getKey())
                .setValue(tagMatcher.getValue())
                .build();
    }

    public static TagMatcher toTagMatcher(Tsaas.TagMatcher tagMatcher) {
        return ImmutableTagMatcher.builder()
                .type(toTagMatcherType(tagMatcher.getType()))
                .key(tagMatcher.getKey())
                .value(tagMatcher.getValue())
                .build();
    }

    public static Tsaas.Metric toMetric(Metric metric) {
        return Tsaas.Metric.newBuilder()
                .setKey(metric.getKey())
                .addAllIntrinsicTags(metric.getIntrinsicTags().stream()
                        .map(GrpcObjectMapper::toTag).collect(Collectors.toSet()))
                .addAllMetaTags(metric.getMetaTags().stream()
                        .map(GrpcObjectMapper::toTag).collect(Collectors.toSet()))
                .addAllExternalTags(metric.getExternalTags().stream()
                        .map(GrpcObjectMapper::toTag).collect(Collectors.toSet()))
                .build();
    }

    public static Metric toMetric(Tsaas.Metric metric) {
        ImmutableMetric.MetricBuilder builder = ImmutableMetric.builder();
        metric.getIntrinsicTagsList().stream()
                .map(GrpcObjectMapper::toTag)
                .forEach(builder::intrinsicTag);
        metric.getMetaTagsList().stream()
                .map(GrpcObjectMapper::toTag)
                .forEach(builder::metaTag);
        metric.getExternalTagsList().stream()
            .map(GrpcObjectMapper::toTag)
            .forEach(builder::externalTag);
        return builder.build();
    }

    // Checks if a metric contains all required tags for OpenNMS
    public static boolean isValid(final Metric metric) {
        if (metric.getFirstTagByKey(MetaTagNames.mtype) == null) {
            LOG.warn("tag mtype is missing in metric, will ignore it: {}", metric);
            return false;
        }
        if (metric.getFirstTagByKey(IntrinsicTagNames.resourceId) == null ) {
            LOG.warn("tag resourceId is missing in metric, will ignore it: {}", metric);
            return false;
        }
        if (metric.getFirstTagByKey(IntrinsicTagNames.name) == null ) {
            LOG.warn("tag name is missing in metric, will ignore it: {}", metric);
            return false;
        }
        return true;
    }

    public static Tsaas.Metrics toMetrics(List<Metric> metrics) {
        List<Tsaas.Metric> grpcMetrics = metrics
                .stream()
                .map(GrpcObjectMapper::toMetric)
                .collect(Collectors.toList());
        return Tsaas.Metrics.newBuilder().addAllMetrics(grpcMetrics).build();
    }

    public static List<Metric> toMetrics(final Tsaas.Metrics metrics) {
        return metrics.getMetricsList()
            .stream()
            .map(GrpcObjectMapper::toMetric)
            .filter(GrpcObjectMapper::isValid) // we want only valid Metrics otherwise there will be a problem in OpenNMS
            .collect(Collectors.toList());
    }

    public static Tsaas.Sample toSample(Sample sample) {
        return Tsaas.Sample.newBuilder()
                .setMetric(toMetric(sample.getMetric()))
                .setTime(toTimestamp(sample.getTime()))
                .setValue(sample.getValue())
                .build();
    }

    public static Sample toSample(Tsaas.Sample sample) {
        return ImmutableSample.builder()
                .metric(toMetric(sample.getMetric()))
                .time(toTimestamp(sample.getTime()))
                .value(sample.getValue())
                .build();
    }

    public static Sample toSample(Metric metric, Tsaas.DataPoint dataPoint) {
        return ImmutableSample.builder()
                .metric(metric)
                .time(toTimestamp(dataPoint.getTime()))
                .value(dataPoint.getValue())
                .build();
    }

    public static Tsaas.DataPoint toDataPoint(DataPoint dataPoint) {
        return Tsaas.DataPoint.newBuilder()
                .setTime(toTimestamp(dataPoint.getTime()))
                .setValue(dataPoint.getValue())
                .build();
    }

    public static List<Sample> toSamples(Tsaas.TimeseriesData timeseriesData) {
        Metric metric = toMetric(timeseriesData.getMetric());
        if(!isValid(metric)) {// we want only valid Metrics otherwise there will be a problem in OpenNMS)
            return Collections.emptyList();
        }
        return timeseriesData
                .getDataPointsList()
                .stream()
                .map(d -> toSample(metric, d))
                .collect(Collectors.toList());
    }

    public static Timestamp toTimestamp(final Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public static Instant toTimestamp(final Timestamp timestamp) {
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos());
    }

    public static TimeSeriesFetchRequest toTimeseriesFetchRequest(final Tsaas.FetchRequest request) {
        return ImmutableTimeSeriesFetchRequest.builder()
                .aggregation(toAggregation(request.getAggregation()))
                .metric(toMetric(request.getMetric()))
                .end(toTimestamp(request.getEnd()))
                .start(toTimestamp(request.getStart()))
                .step(toStep(request.getStep()))
                .build();

    }

    private static Duration toStep(long step) {
        return Duration.ofMillis(step);
    }

    private static Aggregation toAggregation(Tsaas.Aggregation aggregation) {
        if (aggregation == Tsaas.Aggregation.AVERAGE) {
            return Aggregation.AVERAGE;
        } else if (aggregation == MAX){
            return Aggregation.MAX;
        } else if (aggregation == MIN) {
            return Aggregation.MIN;
        } else if (aggregation == NONE) {
            return Aggregation.NONE;
        } else {
            // shouldn't happen, lets set it to none
            return Aggregation.NONE;
        }
    }

    private static TagMatcher.Type toTagMatcherType(Tsaas.TagMatcherType type) {
        if (type == Tsaas.TagMatcherType.EQUALS) {
            return TagMatcher.Type.EQUALS;
        } else if (type == Tsaas.TagMatcherType.NOT_EQUALS) {
            return TagMatcher.Type.NOT_EQUALS;
        } else if (type == Tsaas.TagMatcherType.EQUALS_REGEX) {
            return TagMatcher.Type.EQUALS_REGEX;
        } else if (type == Tsaas.TagMatcherType.NOT_EQUALS_REGEX) {
            return TagMatcher.Type.NOT_EQUALS_REGEX;
        } else {
            // Should never happen
            throw new IllegalArgumentException("Unknown Tsaas.TagMatcherType: " + type);
        }
    }

    private static Tsaas.TagMatcherType toTagMatcherType(TagMatcher.Type type) {
        if (type == TagMatcher.Type.EQUALS) {
            return Tsaas.TagMatcherType.EQUALS;
        } else if (type == TagMatcher.Type.NOT_EQUALS) {
            return Tsaas.TagMatcherType.NOT_EQUALS;
        } else if (type == TagMatcher.Type.EQUALS_REGEX) {
            return Tsaas.TagMatcherType.EQUALS_REGEX;
        } else if (type == TagMatcher.Type.NOT_EQUALS_REGEX) {
            return Tsaas.TagMatcherType.NOT_EQUALS_REGEX;
        } else {
            // Should never happen
            throw new IllegalArgumentException("Unknown TagMatcher.Type: " + type);
        }
    }
}
