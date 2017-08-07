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

public class ActorMethodInvokeExceptionTest {
    @Test
    public void messageInitializedTest() throws Exception {
        Message message = Mockito.mock(Message.class);
        //noinspection ResultOfMethodCallIgnored
        Mockito.doReturn("MESSAGE DESCRIPTION").when(message).toString();
        ActorMethodInvokeException exception = new ActorMethodInvokeException();
        exception.setMessage(this.getClass(), message);
        String exceptionMessage = exception.getMessage();
        // Class name and message representation should be part of exception description
        Assert.assertNotEquals(-1, exceptionMessage.indexOf(getClass().getSimpleName()));
        Assert.assertNotEquals(-1, exceptionMessage.indexOf("MESSAGE DESCRIPTION"));
    }

    @Test
    public void messageNotInitializedTest() throws Exception {
        ActorMethodInvokeException exception = new ActorMethodInvokeException();
        // Should not crash
        String exceptionMessage = exception.getMessage();
        Assert.assertNull(exceptionMessage);
    }
}
