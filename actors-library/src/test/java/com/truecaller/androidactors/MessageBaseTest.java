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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
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
        MessageBase<Object, Object> message = new MockMessageBase<>(mException);
        message.verifyResult(null);
    }

    @Test
    public void verifyResult_returnSame_nonNullResult() {
        MessageBase<Object, Object> message = new MockMessageBase<>(mException);
        Assert.assertSame(mPromise, message.verifyResult(mPromise));
    }

    @Test
    public void logParam_value_nullNoSecurityLevel() {
        Assert.assertEquals("null", MessageBase.logParam(null, SecureParameter.LEVEL_FULL_INFO));
    }

    @Test
    public void logParam_value_nullLevelBasicInfo() {
        Assert.assertEquals("null", MessageBase.logParam(null, SecureParameter.LEVEL_NULL_OR_EMPTY_STRING));
    }

    @Test
    public void logParam_value_nullLevelNoInfo() {
        Assert.assertEquals("<value>", MessageBase.logParam(null, SecureParameter.LEVEL_NO_INFO));
    }

    @Test
    public void logParam_value_primitiveNoSecurityLevel() {
        Assert.assertEquals("10", MessageBase.logParam(10, SecureParameter.LEVEL_FULL_INFO));
    }

    @Test
    public void logParam_value_primitiveLevelBasicInfo() {
        Assert.assertEquals("<not null value>", MessageBase.logParam(10, SecureParameter.LEVEL_NULL_OR_EMPTY_STRING));
    }

    @Test
    public void logParam_value_primitiveLevelNoInfo() {
        Assert.assertEquals("<value>", MessageBase.logParam(10, SecureParameter.LEVEL_NO_INFO));
    }

    @Test
    public void logParam_value_objectNoSecurityLevel() {
        Object value  = new Object() {
            @Override
            public String toString() {
                return "{Test object}";
            }
        };
        Assert.assertEquals("{Test object}", MessageBase.logParam(value, SecureParameter.LEVEL_FULL_INFO));
    }

    @Test
    public void logParam_value_objectLevelBasicInfo() {
        Object value  = new Object() {
            @Override
            public String toString() {
                return "{Test object}";
            }
        };

        Assert.assertEquals("<not null value>", MessageBase.logParam(value, SecureParameter.LEVEL_NULL_OR_EMPTY_STRING));
    }

    @Test
    public void logParam_value_objectLevelNoInfo() {
        Object value  = new Object() {
            @Override
            public String toString() {
                return "{Test object}";
            }
        };
        Assert.assertEquals("<value>", MessageBase.logParam(value, SecureParameter.LEVEL_NO_INFO));
    }

    @Test
    public void logParam_value_notEmptyCharSequenceNoSecurityLevel() {
        StringBuilder value = new StringBuilder("Test string");
        Assert.assertEquals("'Test string'", MessageBase.logParam(value, SecureParameter.LEVEL_FULL_INFO));
    }

    @Test
    public void logParam_value_notEmptyCharSequenceLevelBasicInfo() {
        StringBuilder value = new StringBuilder("Test string");
        Assert.assertEquals("<not empty string>", MessageBase.logParam(value, SecureParameter.LEVEL_NULL_OR_EMPTY_STRING));
    }

    @Test
    public void logParam_value_notEmptyCharSequenceLevelNoInfo() {
        StringBuilder value = new StringBuilder("Test string");
        Assert.assertEquals("<value>", MessageBase.logParam(value, SecureParameter.LEVEL_NO_INFO));
    }

    @Test
    public void logParam_value_emptyCharSequenceNoSecurityLevel() {
        StringBuilder value = new StringBuilder();
        Assert.assertEquals("''", MessageBase.logParam(value, SecureParameter.LEVEL_FULL_INFO));
    }

    @Test
    public void logParam_value_emptyCharSequenceLevelBasicInfo() {
        StringBuilder value = new StringBuilder();
        Assert.assertEquals("''", MessageBase.logParam(value, SecureParameter.LEVEL_NULL_OR_EMPTY_STRING));
    }

    @Test
    public void logParam_value_emptyCharSequenceLevelNoInfo() {
        StringBuilder value = new StringBuilder();
        Assert.assertEquals("<value>", MessageBase.logParam(value, SecureParameter.LEVEL_NO_INFO));
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
