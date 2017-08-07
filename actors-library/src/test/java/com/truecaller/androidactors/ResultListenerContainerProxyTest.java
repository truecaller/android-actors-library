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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class ResultListenerContainerProxyTest {
    private Object mResult = new Object();

    @Mock
    private ResourceCleaner<Object> mCleaner;

    @Mock
    private MessageSender mSender;

    @Mock
    private ResultListenerContainer mContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void deliverTest() {
        ResultListenerContainerProxy proxy = new ResultListenerContainerProxy(mSender);
        proxy.deliverResult(mResult, mCleaner);

        ArgumentCaptor<ResultListenerContainerMessage> captor = ArgumentCaptor.forClass(ResultListenerContainerMessage.class);
        Mockito.verify(mSender).deliver(captor.capture());

        ResultListenerContainerMessage message = captor.getValue();
        Assert.assertNotNull(message);

        // Unfortunately, the only way to get arguments from message - invoke it.
        // It intersects with tests for ResultListenerContainerMessage
        message.invoke(mContainer);
        // If next line fails it can mean two things:
        //   1. ResultListenerContainerMessage is broken, and then ResultListenerContainerMessageTest should also fails
        //   2. We used wrong arguments for ResultListenerContainerMessage, and then this test is failed
        //noinspection unchecked
        Mockito.verify(mContainer).deliverResult(mResult, mCleaner);
    }
}
