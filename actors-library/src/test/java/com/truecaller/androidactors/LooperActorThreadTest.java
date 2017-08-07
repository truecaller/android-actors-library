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

import android.os.HandlerThread;
import android.os.Looper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class LooperActorThreadTest {

    @Mock
    private ProxyFactory mProxyFactory;

    @Mock
    private FailureHandler mFailureHandler;

    @Mock
    private Runnable mProxy;

    private Looper mLooper;

    private ShadowLooper mShadowLooper;

    @Mock
    private Runnable mActorImpl;

    @Before
    public void configure() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(mProxy).when(mProxyFactory).newProxy(Mockito.<Class>any(), Mockito.<MessageSender>any());

        HandlerThread thread = new HandlerThread("text");
        thread.start();
        mLooper = thread.getLooper();
        mShadowLooper = Shadows.shadowOf(mLooper);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void messageDeliverTest() throws Exception {
        final Message message = Mockito.mock(Message.class);

        final LooperActorThread actorThread = new LooperActorThread(mProxyFactory, mFailureHandler, mLooper);
        ActorRef<Runnable> runnableRef = actorThread.bind(Runnable.class, mActorImpl);
        Assert.assertSame(mProxy, runnableRef.tell());

        ArgumentCaptor<MessageSender> postmanCaptor = ArgumentCaptor.forClass(MessageSender.class);
        Mockito.verify(mProxyFactory).newProxy(Mockito.<Class>any(), postmanCaptor.capture());

        MessageSender postman = postmanCaptor.getValue();
        postman.deliver(message);
        // check that we did nothing on same thread
        Mockito.verify(message, Mockito.never()).invoke(Mockito.any());
        // Process all scheduled messages on looper thread and check that our actor's method was called
        mShadowLooper.runToEndOfTasks();
        Mockito.verify(message).invoke(mActorImpl);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void messageExceptionTest() throws Exception {
        final Throwable occurredException = new RuntimeException();
        final ActorMethodInvokeException baseCallException = Mockito.mock(ActorMethodInvokeException.class);
        final Message message = Mockito.mock(Message.class);
        Mockito.doThrow(occurredException).when(message).invoke(mActorImpl);
        //noinspection ThrowableNotThrown
        Mockito.doReturn(baseCallException).when(message).exception();

        final LooperActorThread actorThread = new LooperActorThread(mProxyFactory, mFailureHandler, mLooper);
        ActorRef<Runnable> runnableRef = actorThread.bind(Runnable.class, mActorImpl);
        Assert.assertSame(mProxy, runnableRef.tell());

        ArgumentCaptor<MessageSender> postmanCaptor = ArgumentCaptor.forClass(MessageSender.class);
        Mockito.verify(mProxyFactory).newProxy(Mockito.<Class>any(), postmanCaptor.capture());

        MessageSender postman = postmanCaptor.getValue();
        postman.deliver(message);
        // check that we did nothing on same thread
        Mockito.verify(message, Mockito.never()).invoke(Mockito.any());
        // Process all scheduled messages on looper thread and check that our actor's method was called
        mShadowLooper.runToEndOfTasks();
        // Check that exception was reported to FailureHandler
        Mockito.verify(mFailureHandler).onUncaughtException(mActorImpl, message, baseCallException);
        Mockito.verify(baseCallException).initCause(occurredException);
    }
}
