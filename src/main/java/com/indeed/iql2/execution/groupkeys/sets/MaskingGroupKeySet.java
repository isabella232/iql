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

package com.indeed.iql2.execution.groupkeys.sets;

import com.indeed.iql2.execution.groupkeys.GroupKey;

import java.util.BitSet;
import java.util.Objects;

public class MaskingGroupKeySet implements GroupKeySet {
    private final GroupKeySet wrapped;
    private final BitSet presentMask;

    public MaskingGroupKeySet(GroupKeySet wrapped, BitSet presentMask) {
        this.wrapped = wrapped;
        this.presentMask = presentMask;
    }

    @Override
    public GroupKeySet previous() {
        return wrapped.previous();
    }

    @Override
    public int parentGroup(int group) {
        return wrapped.parentGroup(group);
    }

    @Override
    public GroupKey groupKey(int group) {
        return wrapped.groupKey(group);
    }

    @Override
    public int numGroups() {
        return wrapped.numGroups();
    }

    @Override
    public boolean isPresent(int group) {
        return group > 0 && group < presentMask.size() && presentMask.get(group);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaskingGroupKeySet that = (MaskingGroupKeySet) o;
        return Objects.equals(wrapped, that.wrapped) &&
                Objects.equals(presentMask, that.presentMask);
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrapped, presentMask);
    }
}