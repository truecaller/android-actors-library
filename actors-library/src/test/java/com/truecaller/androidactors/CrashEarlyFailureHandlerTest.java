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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class CrashEarlyFailureHandlerTest {

    private static final String METHOD_SIGNATURE = ".A()";

    private final Object mImpl = new Object();

    @Mock
    private Message mMessage;

    @Mock
    private ActorMethodInvokeException mThrowable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        //noinspection ResultOfMethodCallIgnored
        Mockito.doReturn(METHOD_SIGNATURE).when(mMessage).toString();
    }

    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
    @Test
    public void crashIfMessageFailedTest() {
        final CrashEarlyFailureHandler handler = new CrashEarlyFailureHandler();
        boolean thrown = false;

        try {
            handler.onUncaughtException(mImpl, mMessage, mThrowable);
        } catch (RuntimeException e) {
            thrown = true;
            Assert.assertSame(mThrowable, e);
        }
        Mockito.verify(mThrowable).setMethodSignature(mImpl.getClass(), mMessage);
        Assert.assertTrue(thrown);
    }
}
