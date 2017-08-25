/*
 * Copyright (C) 2017 True Software Scandinavia AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.truecaller.androidactors;

import com.truecaller.androidactors.NamesProviderImpl.NamesComparator;
import org.junit.Assert;
import org.junit.Test;

public class NamesProviderImplTest {
    @Test
    public void compare_lessThanZero_leftIsNull() {
        NamesComparator comparator = new NamesComparator();
        Assert.assertTrue(comparator.compare(null, "123456") < 0);
    }

    @Test
    public void compare_greaterThanZero_leftIsNull() {
        NamesComparator comparator = new NamesComparator();
        Assert.assertTrue(comparator.compare("1234", null) > 0);
    }

    @Test
    public void compare_zero_nulls() {
        NamesComparator comparator = new NamesComparator();
        Assert.assertTrue(comparator.compare(null, null) == 0);
    }

    @Test
    public void compare_lessThanZero_leftShorterThanRight() {
        NamesComparator comparator = new NamesComparator();
        Assert.assertTrue(comparator.compare("1234", "123456") < 0);
    }

    @Test
    public void compare_greaterThanZero_leftLongerThanRight() {
        NamesComparator comparator = new NamesComparator();
        Assert.assertTrue(comparator.compare("123456", "1234") > 0);
    }

    @Test
    public void compare_zero_equals() {
        NamesComparator comparator = new NamesComparator();
        Assert.assertTrue(comparator.compare("asdfg", "asdfg") == 0);
    }

    @Test
    public void compare_lessThanZero_leftLessThanRight() {
        NamesComparator comparator = new NamesComparator();
        Assert.assertTrue(comparator.compare("asdfg", "asefg") < 0);
    }

    @Test
    public void compare_greaterThanZero_leftGreaterThanRight() {
        NamesComparator comparator = new NamesComparator();
        Assert.assertTrue(comparator.compare("1238", "1234") > 0);
    }

    @Test
    public void buildMessageName_sameName_uniqueName() {
        NamesProviderImpl namesProvider = new NamesProviderImpl();
        Assert.assertEquals("TestMethodMessage", namesProvider.buildMessageName("testMethod"));
    }

    @Test
    public void buildMessageName_sameNameWithCounter_nonUniqueName() {
        NamesProviderImpl namesProvider = new NamesProviderImpl();
        namesProvider.buildMessageName("testMethod");
        Assert.assertEquals("TestMethodMessage1", namesProvider.buildMessageName("testMethod"));
    }
}
