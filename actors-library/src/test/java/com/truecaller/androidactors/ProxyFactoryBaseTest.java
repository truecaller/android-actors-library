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
import org.mockito.Mockito;

public class ProxyFactoryBaseTest {
    @Test(expected = IllegalArgumentException.class)
    public void unknownClassTest() {
        ProxyFactoryBase factoryBase = Mockito.mock(ProxyFactoryBase.class, Mockito.CALLS_REAL_METHODS);
        factoryBase.defaultProxy(ProxyFactoryBaseTest.class, Mockito.mock(MessageSender.class));
    }

    @Test
    public void listenerContainerTest() {
        ProxyFactoryBase factoryBase = Mockito.mock(ProxyFactoryBase.class, Mockito.CALLS_REAL_METHODS);
        ResultListenerContainer proxy = factoryBase.defaultProxy(ResultListenerContainer.class, Mockito.mock(MessageSender.class));
        Assert.assertNotNull(proxy);
        Assert.assertTrue(proxy instanceof ResultListenerContainerProxy);
    }
}
