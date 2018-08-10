/*
 * Copyright (C) 2018 Indeed Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.indeed.iql2.language.commands;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.indeed.iql2.execution.groupkeys.sets.GroupKeySet;
import com.indeed.iql2.execution.metrics.aggregate.PerGroupConstant;
import com.indeed.iql2.language.AggregateMetric;

import java.util.Objects;

public class TopK {
    public final Optional<Long> limit;
    public final Optional<AggregateMetric> metric;

    public TopK(Optional<Long> limit, Optional<AggregateMetric> metric) {
        this.limit = limit;
        this.metric = metric;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopK topK = (TopK) o;
        return Objects.equals(limit, topK.limit) &&
                Objects.equals(metric, topK.metric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, metric);
    }

    @Override
    public String toString() {
        return "TopK{" +
                "limit=" + limit +
                ", metric=" + metric +
                '}';
    }

    public com.indeed.iql2.execution.commands.misc.TopK toExecution(Function<String, PerGroupConstant> namedMetricLookup, GroupKeySet groupKeySet) {
        return new com.indeed.iql2.execution.commands.misc.TopK(
                limit.transform(x -> (int)(long)x),
                metric.transform(x -> x.toExecutionMetric(namedMetricLookup, groupKeySet))
        );
    }
}