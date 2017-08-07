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

import com.truecaller.androidactors.ExecutorActorThread.DeliverRunnable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class ExecutorActorThreadTest {
    @Mock
    private Executor mExecutor;

    @Mock
    private ProxyFactory mProxyFactory;

    @Mock
    private FailureHandler mFailureHandler;

    @Mock
    private Runnable mActorImpl;

    @Mock
    private Runnable mProxy;

    private ExecutorActorThread mThread;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        Mockito.doReturn(mProxy).when(mProxyFactory).newProxy(Mockito.<Class>any(), Mockito.<MessageSender>any());

        mThread = new ExecutorActorThread(mExecutor, mProxyFactory, mFailureHandler);

        DeliverRunnable.clearPool();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void messageDeliverTest() throws Exception {
        ActorRef<Runnable> runnableRef = mThread.bind(Runnable.class, mActorImpl);
        // Check that we provided correct proxy class
        Assert.assertSame(mProxy, runnableRef.tell());
        // Grab postman and check it
        ArgumentCaptor<MessageSender> postmanCaptor = ArgumentCaptor.forClass(MessageSender.class);
        Mockito.verify(mProxyFactory).newProxy(Mockito.<Class>any(), postmanCaptor.capture());

        final Message message = Mockito.mock(Message.class);
        final MessageSender postman = postmanCaptor.getValue();
        postman.deliver(message);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(mExecutor).execute(runnableCaptor.capture());
        Runnable runnable = runnableCaptor.getValue();
        Assert.assertNotNull(runnable);

        // Check that we tried to invoke method
        runnable.run();

        Mockito.verify(message).invoke(mActorImpl);
    }

    @SuppressWarnings({"unchecked", "ThrowableNotThrown"})
    @Test
    public void messageExceptionTest() throws Exception {
        ActorRef<Runnable> runnableRef = mThread.bind(Runnable.class, mActorImpl);
        // Check that we provided correct proxy class
        Assert.assertSame(mProxy, runnableRef.tell());
        // Grab postman and check it
        ArgumentCaptor<MessageSender> postmanCaptor = ArgumentCaptor.forClass(MessageSender.class);
        Mockito.verify(mProxyFactory).newProxy(Mockito.<Class>any(), postmanCaptor.capture());

        final Message message = Mockito.mock(Message.class);
        final RuntimeException exception = Mockito.mock(RuntimeException.class);
        final ActorMethodInvokeException callException = Mockito.mock(ActorMethodInvokeException.class);
        Mockito.doThrow(exception).when(message).invoke(Mockito.any());
        Mockito.doReturn(callException).when(message).exception();
        final MessageSender postman = postmanCaptor.getValue();
        postman.deliver(message);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify(mExecutor).execute(runnableCaptor.capture());
        Runnable runnable = runnableCaptor.getValue();
        Assert.assertNotNull(runnable);

        // Check that we tried to invoke method
        runnable.run();
        // Check that we pass exception to handler
        Mockito.verify(mFailureHandler).onUncaughtException(mActorImpl, message, callException);
        Mockito.verify(callException).initCause(exception);
    }

    @Test
    public void runnableRecycleTest() throws Exception {
        final Message message = Mockito.mock(Message.class);
        DeliverRunnable runnable = DeliverRunnable.obtain(mActorImpl, message, mFailureHandler);

        Assert.assertSame(mFailureHandler, runnable.failureHandler);
        Assert.assertSame(mActorImpl, runnable.impl);
        Assert.assertSame(message, runnable.message);

        runnable.run();

        // Runnable should be cleaned
        Assert.assertNull(runnable.failureHandler);
        Assert.assertNull(runnable.impl);
        Assert.assertNull(runnable.message);

        // Check that we will receive same object
        DeliverRunnable nextRunnable = DeliverRunnable.obtain(mActorImpl, message, mFailureHandler);
        Assert.assertSame(runnable, nextRunnable);
    }

    @SuppressWarnings({"ThrowableNotThrown", "unchecked"})
    @Test
    public void runnableExceptionRecycleTest() throws Exception {
        final Message message = Mockito.mock(Message.class);
        DeliverRunnable runnable = DeliverRunnable.obtain(mActorImpl, message, mFailureHandler);

        Assert.assertSame(mFailureHandler, runnable.failureHandler);
        Assert.assertSame(mActorImpl, runnable.impl);
        Assert.assertSame(message, runnable.message);

        Mockito.doReturn(new ActorMethodInvokeException()).when(message).exception();
        Mockito.doThrow(new RuntimeException()).when(message).invoke(Mockito.any());
        runnable.run();

        // Runnable should be cleaned
        Assert.assertNull(runnable.failureHandler);
        Assert.assertNull(runnable.impl);
        Assert.assertNull(runnable.message);

        // Check that we will receive same object
        DeliverRunnable nextRunnable = DeliverRunnable.obtain(mActorImpl, message, mFailureHandler);
        Assert.assertSame(runnable, nextRunnable);
    }

    @Test
    public void poolCapacityTest() throws Exception {
        final Message message = Mockito.mock(Message.class);

        List<DeliverRunnable> items = new ArrayList<>();
        for (int index = 0; index < DeliverRunnable.MAX_POOL_SIZE + 1; ++index) {
            items.add(DeliverRunnable.obtain(mActorImpl, message, mFailureHandler));
        }

        for(DeliverRunnable item : items) {
            item.recycle();
        }

        // Check that we saved runnables to pull
        for (int index = DeliverRunnable.MAX_POOL_SIZE - 1; index >= 0; --index) {
            DeliverRunnable item = DeliverRunnable.obtain(mActorImpl, message, mFailureHandler);
            Assert.assertSame(items.get(index), item);
        }

        // Last one shouldn't be the same
        DeliverRunnable item = DeliverRunnable.obtain(mActorImpl, message, mFailureHandler);
        Assert.assertNotSame(items.get(DeliverRunnable.MAX_POOL_SIZE), item);
    }
}
