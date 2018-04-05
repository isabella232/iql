package com.indeed.squall.iql2.execution.metrics.aggregate;

import com.indeed.squall.iql2.execution.QualifiedPush;
import com.indeed.squall.iql2.execution.groupkeys.sets.GroupKeySet;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DocumentLevelMetric implements AggregateMetric {
    private final String sessionName;
    private final List<String> pushes;
    private int index = -1;

    public DocumentLevelMetric(final String sessionName, final List<String> pushes) {
        this.sessionName = sessionName;
        this.pushes = pushes;
    }

    @Override
    public Set<QualifiedPush> requires() {
        return Collections.singleton(new QualifiedPush(sessionName, pushes));
    }

    @Override
    public void register(final Map<QualifiedPush, Integer> metricIndexes, final GroupKeySet groupKeySet) {
        this.index = metricIndexes.get(new QualifiedPush(sessionName, pushes));
    }

    @Override
    public double[] getGroupStats(final long[][] stats, final int numGroups) {
        final double[] result = new double[numGroups + 1];
        for (int i = 0; i < Math.min(result.length, stats[index].length); i++) {
            result[i] = (double) stats[index][i];
        }
        return result;
    }

    @Override
    public double apply(final String term, final long[] stats, final int group) {
        return stats[index];
    }

    @Override
    public double apply(final long term, final long[] stats, final int group) {
        return stats[index];
    }

    @Override
    public boolean needGroup() {
        return false;
    }

    @Override
    public boolean needStats() {
        return true;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return "DocumentLevelMetric{" +
                "sessionName='" + sessionName + '\'' +
                ", pushes=" + pushes +
                ", index=" + index +
                '}';
    }
}