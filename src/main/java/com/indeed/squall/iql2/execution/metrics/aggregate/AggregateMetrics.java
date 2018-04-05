package com.indeed.squall.iql2.execution.metrics.aggregate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.indeed.squall.iql2.execution.AggregateFilter;
import com.indeed.squall.iql2.execution.AggregateFilters;
import com.indeed.squall.iql2.execution.groupkeys.sets.GroupKeySet;

import java.util.ArrayList;
import java.util.List;

public class AggregateMetrics {
    public static AggregateMetric fromJson(final JsonNode node, final Function<String, PerGroupConstant> namedMetricLookup, final GroupKeySet groupKeySet) {
        final Supplier<AggregateMetric> m1 = new Supplier<AggregateMetric>() {
            public AggregateMetric get() {
                return fromJson(node.get("m1"), namedMetricLookup, groupKeySet);
            }
        };
        final Supplier<AggregateMetric> m2 = new Supplier<AggregateMetric>() {
            public AggregateMetric get() {
                return fromJson(node.get("m2"), namedMetricLookup, groupKeySet);
            }
        };
        final Supplier<AggregateMetric> value = new Supplier<AggregateMetric>() {
            public AggregateMetric get() {
                return fromJson(node.get("value"), namedMetricLookup, groupKeySet);
            }
        };
        switch (node.get("type").textValue()) {
            case "docStats": {
                return createPushMetric(node.get("sessionName").textValue(), node.get("pushes"));
            }
            case "groupStatsLookup": {
                return namedMetricLookup.apply(node.get("name").textValue());
            }
            case "groupStatsMultiLookup": {
                final JsonNode nameNode = node.get("names");
                final List<double[]> values = new ArrayList<>();
                for (int i = 0; i < nameNode.size(); i++) {
                    final String name = nameNode.get(i).textValue();
                    values.add(namedMetricLookup.apply(name).values);
                }
                return new MultiPerGroupConstant(values);
            }
            case "addition":
                return new Add(m1.get(), m2.get());
            case "subtraction":
                return new Subtract(m1.get(), m2.get());
            case "division":
                return new Divide(m1.get(), m2.get());
            case "multiplication":
                return new Multiply(m1.get(), m2.get());
            case "power":
                return new Power(m1.get(), m2.get());
            case "abs":
                return new Abs(value.get());
            case "signum":
                return new Signum(value.get());
            case "log":
                return new Log(value.get());
            case "constant":
                return new Constant(node.get("value").doubleValue());
            case "perGroupConstants": {
                final JsonNode constantsArray = node.get("values");
                final double[] perGroupConstants = new double[constantsArray.size() + 1];
                for (int i = 0; i < constantsArray.size(); i++) {
                    perGroupConstants[i + 1] = constantsArray.get(i).doubleValue();
                }
                return new PerGroupConstant(perGroupConstants);
            }
            case "lag": {
                final int delay = node.get("delay").intValue();
                final AggregateMetric metric = fromJson(node.get("m"), namedMetricLookup, groupKeySet);
                return new ParentLag(delay, metric);
            }
            case "iterateLag": {
                final int delay = node.get("delay").intValue();
                final AggregateMetric metric = fromJson(node.get("m"), namedMetricLookup, groupKeySet);
                return new IterateLag(delay, metric);
            }
            case "modulus": {
                return new Modulus(m1.get(), m2.get());
            }
            case "running": {
                return new Running(value.get(), node.get("offset").intValue());
            }
            case "window": {
                return new Window(node.get("size").intValue(), value.get());
            }
            case "sumChildren": {
                return new SumChildren(value.get());
            }
            case "ifThenElse": {
                final AggregateFilter condition = AggregateFilters.fromJson(node.get("condition"), namedMetricLookup, groupKeySet);
                final AggregateMetric trueCase = fromJson(node.get("trueCase"), namedMetricLookup, groupKeySet);
                final AggregateMetric falseCase = fromJson(node.get("falseCase"), namedMetricLookup, groupKeySet);
                return new IfThenElse(condition, trueCase, falseCase);
            }
            case "min": {
                final List<AggregateMetric> metrics = new ArrayList<>();
                final JsonNode jsonMetrics = node.get("metrics");
                for (int i = 0; i < jsonMetrics.size(); i++) {
                    metrics.add(fromJson(jsonMetrics.get(i), namedMetricLookup, groupKeySet));
                }
                return new Min(metrics);
            }
            case "max": {
                final List<AggregateMetric> metrics = new ArrayList<>();
                final JsonNode jsonMetrics = node.get("metrics");
                for (int i = 0; i < jsonMetrics.size(); i++) {
                    metrics.add(fromJson(jsonMetrics.get(i), namedMetricLookup, groupKeySet));
                }
                return new Max(metrics);
            }
            case "negate": {
                return new Subtract(new Constant(0), value.get());
            }
        }
        throw new RuntimeException("Oops: " + node);
    }

    public static AggregateMetric createPushMetric(String sessionName, JsonNode pushes1) {
        final List<String> pushes = Lists.newArrayList();
        for (final JsonNode push : pushes1) {
            pushes.add(push.textValue());
        }
        return new DocumentLevelMetric(sessionName, pushes);
    }
}