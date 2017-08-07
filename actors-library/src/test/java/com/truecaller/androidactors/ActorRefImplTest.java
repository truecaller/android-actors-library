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

import org.junit.Assert;
import org.junit.Test;

public class ActorRefImplTest {
    /**
     * Just check that reference is always the same
     */
    @Test
    public void referenceTest() {
        Long item = 12000000L;
        ActorRefImpl<Long> ref = new ActorRefImpl<>(item);
        Assert.assertSame(item, ref.tell());
        Assert.assertSame(item, ref.tell());
    }
}
