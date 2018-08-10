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

package com.indeed.iql2.server.web.servlets;

import com.google.common.collect.ImmutableList;
import com.indeed.iql2.server.web.servlets.dataset.MultipleDataset;
import com.indeed.iql2.server.web.servlets.dataset.OrganicDataset;
import org.junit.Test;

import java.util.List;

import static com.indeed.iql2.server.web.servlets.QueryServletTestUtils.testAll;
import static com.indeed.iql2.server.web.servlets.QueryServletTestUtils.testIQL1;
import static com.indeed.iql2.server.web.servlets.QueryServletTestUtils.testIQL2;

public class BasicFilterTest extends BasicTest {
    @Test
    public void testBasicFilters() throws Exception {
        QueryServletTestUtils.testAll(OrganicDataset.create(), ImmutableList.<List<String>>of(ImmutableList.of("", "4")), "from organic yesterday today where tk=\"a\" select count()");
        QueryServletTestUtils.testAll(OrganicDataset.create(), ImmutableList.<List<String>>of(ImmutableList.of("", "2")), "from organic yesterday today where tk=\"b\" select count()");
        QueryServletTestUtils.testAll(OrganicDataset.create(), ImmutableList.<List<String>>of(ImmutableList.of("", "4")), "from organic yesterday today where tk=\"c\" select count()");
        QueryServletTestUtils.testAll(OrganicDataset.create(), ImmutableList.<List<String>>of(ImmutableList.of("", "141")), "from organic yesterday today where tk=\"d\" select count()");
        QueryServletTestUtils.testIQL1(OrganicDataset.create(), ImmutableList.of(ImmutableList.of("", "141")), "from organic yesterday today where tk:d select count()");
    }

    @Test
    public void testMultipleDatasetFilters() throws Exception {
        QueryServletTestUtils.testIQL2(MultipleDataset.create(), ImmutableList.of(ImmutableList.of("", "2")), "from dataset1 yesterday today, dataset2 where intField1=1 select count()");
        QueryServletTestUtils.testIQL2(MultipleDataset.create(), ImmutableList.of(ImmutableList.of("", "22")), "from dataset1 1month today, dataset2 where strField1='1' select count()");
        QueryServletTestUtils.testIQL2(MultipleDataset.create(), ImmutableList.of(ImmutableList.of("", "3")), "from dataset1 1w today, dataset2 where dataset1.intField2=1 select dataset1.count()");
    }

    @Test
    public void testNegateFilters() throws Exception {
        QueryServletTestUtils.testAll(OrganicDataset.create(), ImmutableList.of(ImmutableList.of("", "149")), "from organic yesterday today where NOT(tk=\"b\") select count()");
        QueryServletTestUtils.testIQL1(OrganicDataset.create(), ImmutableList.of(ImmutableList.of("", "147")), "from organic yesterday today where tk!=\"a\" select count()");
        QueryServletTestUtils.testIQL1(OrganicDataset.create(), ImmutableList.of(ImmutableList.of("", "147")), "from organic yesterday today where -tk:c select count()");
        QueryServletTestUtils.testIQL1(OrganicDataset.create(), ImmutableList.of(ImmutableList.of("", "10")), "from organic yesterday today where -tk=~'d' select count()");
    }

}