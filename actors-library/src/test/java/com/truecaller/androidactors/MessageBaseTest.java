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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MessageBaseTest {
    @Mock
    private ActorInvokeException mException;

    @Mock
    private Promise<Object> mPromise;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void exception_notNull_getException() {
        MessageBase message = new MockMessageBase(mException);

        Assert.assertSame(mException, message.exception());
    }

    @Test(expected = AssertionError.class)
    public void verifyResult_throw_nullResult() {
        MessageBase<Object, Object> message = new MockMessageBase<>(mException) ;
        message.verifyResult(null);
    }

    @Test
    public void verifyResult_returnSame_nonNullResult() {
        MessageBase<Object, Object> message = new MockMessageBase<>(mException) ;
        Assert.assertSame(mPromise, message.verifyResult(mPromise));
    }

    private static class MockMessageBase<T, R> extends MessageBase<T, R> {
        private MockMessageBase(@NonNull ActorInvokeException exception) {
            super(exception);
        }

        @Nullable
        @Override
        public Promise<R> invoke(@NonNull T target) {
            return null;
        }
    }
}
