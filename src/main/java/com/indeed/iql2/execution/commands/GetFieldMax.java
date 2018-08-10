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

package com.indeed.iql2.execution.commands;

import com.indeed.imhotep.api.ImhotepOutOfMemoryException;
import com.indeed.iql2.execution.QualifiedPush;
import com.indeed.iql2.execution.Session;
import com.indeed.iql2.execution.commands.misc.IterateHandler;
import com.indeed.iql2.execution.commands.misc.IterateHandlerable;
import com.indeed.iql2.execution.commands.misc.IterateHandlers;
import com.indeed.iql2.execution.compat.Consumer;
import com.indeed.iql2.execution.groupkeys.sets.GroupKeySet;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class GetFieldMax implements IterateHandlerable<long[]>, Command {
    private static final Logger log = Logger.getLogger(GetFieldMax.class);

    public final Set<String> scope;
    public final String field;

    public GetFieldMax(Set<String> scope, String field) {
        this.scope = scope;
        this.field = field;
    }

    @Override
    public void execute(Session session, Consumer<String> out) throws ImhotepOutOfMemoryException, IOException {
        final long[] result = IterateHandlers.executeSingle(session, field, iterateHandler(session));
        out.accept(Session.MAPPER.writeValueAsString(result));
    }

    @Override
    public IterateHandler<long[]> iterateHandler(Session session) throws ImhotepOutOfMemoryException, IOException {
        return new IterateHandlerImpl(session.numGroups);
    }

    private class IterateHandlerImpl implements IterateHandler<long[]> {
        private long[] max;

        public IterateHandlerImpl(int numGroups) {
            max = new long[numGroups];
            Arrays.fill(max, Long.MIN_VALUE);
        }

        @Override
        public Set<String> scope() {
            return scope;
        }

        @Override
        public Session.IntIterateCallback intIterateCallback() {
            return new Session.IntIterateCallback() {
                @Override
                public void term(final long term, final long[] stats, final int group) {
                    max[group - 1] = Math.max(max[group - 1], term);
                }

                @Override
                public boolean needSorted() {
                    return false;
                }

                @Override
                public boolean needGroup() {
                    return true;
                }

                @Override
                public boolean needStats() {
                    return false;
                }
            };
        }

        @Override
        public Session.StringIterateCallback stringIterateCallback() {
            return new Session.StringIterateCallback() {
                @Override
                public void term(final String term, final long[] stats, final int group) {
                    try {
                        final long v = Long.parseLong(term);
                        max[group - 1] = Math.max(max[group - 1], v);
                    } catch (final NumberFormatException ignored) {
                    }
                }

                @Override
                public boolean needSorted() {
                    return false;
                }

                @Override
                public boolean needGroup() {
                    return true;
                }

                @Override
                public boolean needStats() {
                    return false;
                }
            };
        }

        @Override
        public long[] finish() throws ImhotepOutOfMemoryException, IOException {
            return max;
        }

        @Override
        public Set<QualifiedPush> requires() {
            return Collections.emptySet();
        }

        @Override
        public void register(Map<QualifiedPush, Integer> metricIndexes, GroupKeySet groupKeySet) {
        }
    }
}