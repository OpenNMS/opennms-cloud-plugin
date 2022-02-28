package org.opennms.plugins.cloud.tsaas.grpc;

import static org.opennms.tsaas.Tsaas.Aggregation.MAX;
import static org.opennms.tsaas.Tsaas.Aggregation.MIN;
import static org.opennms.tsaas.Tsaas.Aggregation.NONE;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.opennms.integration.api.v1.timeseries.Aggregation;
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

import com.google.protobuf.Timestamp;

public class GrpcObjectMapper {

    public static Collection<Tag> toTags(Collection<Tsaas.Tag> tags) {
        return tags.stream()
                .map(t -> new ImmutableTag(t.getKey(), t.getValue()))
                .collect(Collectors.toList());
    }

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

    public static List<Sample> toSamples(Tsaas.Samples samples) {
        return samples
                .getSamplesList()
                .stream()
                .map(GrpcObjectMapper::toSample)
                .collect(Collectors.toList());
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                // TODO: Is this the correct way to craft a proto timestamp from an instant?
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
