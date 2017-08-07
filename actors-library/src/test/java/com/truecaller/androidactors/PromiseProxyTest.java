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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.CountDownLatch;

@SuppressWarnings("unchecked")
public class PromiseProxyTest {
    @Mock
    private MessageSender mSender;

    @Mock
    private MessageWithResult mMessage;

    @Mock
    private PromiseImpl mPromiseImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void invokeTest() {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        final Object impl = new Object();
        Mockito.reset(mMessage);
        promise.invoke(impl);
        Mockito.verify(mMessage).invoke(impl, promise);
    }

    @Test
    public void thenNothingTest() {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        Mockito.verifyZeroInteractions(mSender, mMessage);
        promise.thenNothing();
        Mockito.verify(mSender).deliver(promise);
    }

    @Test
    public void thenSameThreadTest() {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        Mockito.verifyZeroInteractions(mSender, mMessage);
        promise.then(Mockito.mock(ResultListener.class));
        Mockito.verify(mSender).deliver(promise);
    }

    @Test
    public void thenActorThreadThreadTest() {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        Mockito.verifyZeroInteractions(mSender, mMessage);
        promise.then(Mockito.mock(ActorThread.class), Mockito.mock(ResultListener.class));
        Mockito.verify(mSender).deliver(promise);
    }

    @Test
    public void delegateWithoutListenerTest() {
        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.dispatch(mPromiseImpl);
        Mockito.verify(mPromiseImpl).deliver(promise);
        Mockito.verifyNoMoreInteractions(mPromiseImpl);
    }

    @Test
    public void delegateWithListenerTest() {
        final ResultListener listener = Mockito.mock(ResultListener.class);

        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(listener);
        promise.dispatch(mPromiseImpl);
        Mockito.verify(mPromiseImpl).deliver(promise);
        Mockito.verifyNoMoreInteractions(mPromiseImpl);
        /*// link to listener should be cleaned, but the only way to check it - call delegate second time
        Mockito.reset(mPromiseImpl);
        promise.dispatch(mPromiseImpl);
        Mockito.verify(mPromiseImpl).then(null);
        Mockito.verifyNoMoreInteractions(mPromiseImpl);*/
    }

    @Test
    public void delegateWithListenerAndThreadTest() {
        final ResultListener listener = Mockito.mock(ResultListener.class);
        final ActorThread thread = Mockito.mock(ActorThread.class);

        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(thread, listener);
        promise.dispatch(mPromiseImpl);
        Mockito.verify(mPromiseImpl).deliver(thread, promise);
        Mockito.verifyNoMoreInteractions(mPromiseImpl);
    }

    @Test
    public void forgetListenerTest() {
        final ResultListener listener = Mockito.mock(ResultListener.class);

        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        ActionHandle handle = promise.then(listener);
        handle.forget();
        promise.dispatch(mPromiseImpl);
        // Even if we don't have link to listener we still must deliver result
        Mockito.verify(mPromiseImpl).deliver(promise);
        Mockito.verifyNoMoreInteractions(mPromiseImpl);
    }

    @Test
    public void toStringTest() {
        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        //noinspection ResultOfMethodCallIgnored
        Mockito.doReturn("Message description string").when(mMessage).toString();
        Assert.assertEquals("Message description string", promise.toString());
    }

    @Test(timeout = 4000)
    public void blockListenerResultDeliveryTest() throws Exception {
        final PromiseProxy.BlockResultListener<String> listener = new PromiseProxy.BlockResultListener<>();
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = listener.waitAndGet();
                    Assert.assertEquals("Result text", result);
                    latch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        // Wait until thread starts (can be replaced by semaphore/latch)
        Thread.sleep(200);
        listener.onResult("Result text");
        latch.await();
    }

    @Test(timeout = 4000)
    public void blockListenerNullDeliveryTest() throws Exception {
        final PromiseProxy.BlockResultListener<String> listener = new PromiseProxy.BlockResultListener<>();
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = listener.waitAndGet();
                    Assert.assertNull(result);
                    latch.countDown();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        // Wait until thread starts (can be replaced by semaphore/latch)
        Thread.sleep(200);
        listener.onResult(null);
        latch.await();
    }

    @Test(timeout = 3000)
    public void getResultTest() throws Exception {

        final String expected = "Call result";

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ResultListenerContainer<String> listener = (ResultListenerContainer<String>) invocation.getArguments()[0];
                listener.deliverResult(expected, null);
                return null;
            }
        }).when(mPromiseImpl).deliver(Mockito.<ResultListenerContainer>any());

        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PromiseProxy proxy = (PromiseProxy) invocation.getArguments()[0];
                proxy.dispatch(mPromiseImpl);
                return null;
            }
        }).when(mSender).deliver(Mockito.<Message>any());

        final PromiseProxy<Object, String> promise = new PromiseProxy<>(mSender, mMessage);
        String result = promise.get();
        Assert.assertEquals(expected, result);
    }

    @Test
    @SuppressWarnings("ThrowableNotThrown")
    public void exceptionProxyTest() {
        ActorMethodInvokeException exception = Mockito.mock(ActorMethodInvokeException.class);
        Mockito.doReturn(exception).when(mMessage).exception();
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        Assert.assertSame(exception, promise.exception());
    }

    @Test(expected = AssertionError.class)
    public void deliverWithoutThread() {
        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.deliver(Mockito.mock(ResultListenerContainer.class));
    }

    @Test(expected = AssertionError.class)
    public void deliverWithThread() {
        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.deliver(Mockito.mock(ActorThread.class), Mockito.mock(ResultListenerContainer.class));
    }

    @Test
    public void deliverResultTest() {
        final ResultListener listener = Mockito.mock(ResultListener.class);
        final ActorThread thread = Mockito.mock(ActorThread.class);
        final Object result = new Object();

        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(thread, listener);

        promise.deliverResult(result, null);

        Mockito.verify(listener).onResult(result);
        // link to listener should be cleaned, but the only way to check it - call delegate second time
        promise.deliverResult(result, null);
        Mockito.verifyNoMoreInteractions(listener);
    }

    @Test
    public void deliverNullResultTest() {
        final ResultListener listener = Mockito.mock(ResultListener.class);
        final ActorThread thread = Mockito.mock(ActorThread.class);

        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(thread, listener);

        promise.deliverResult(null, null);

        Mockito.verify(listener).onResult(null);
        // link to listener should be cleaned, but the only way to check it - call delegate second time
        promise.deliverResult(null, null);
        Mockito.verifyNoMoreInteractions(listener);
    }

    @Test
    public void ignoreResultTest() {
        final ResultListener listener = Mockito.mock(ResultListener.class);
        final ActorThread thread = Mockito.mock(ActorThread.class);

        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(thread, listener);
        promise.forget();

        promise.deliverResult(null, null);

        Mockito.verifyZeroInteractions(listener);
    }

    @Test
    public void cleanResultTest() {
        final ResultListener listener = Mockito.mock(ResultListener.class);
        final ActorThread thread = Mockito.mock(ActorThread.class);
        final Object result = new Object();
        final ResourceCleaner cleaner = Mockito.mock(ResourceCleaner.class);

        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(thread, listener);
        promise.forget();

        promise.deliverResult(result, cleaner);

        Mockito.verifyZeroInteractions(listener);
        Mockito.verify(cleaner).clean(result);
    }

    @Test
    public void cleanNullResultTest() {
        final ResultListener listener = Mockito.mock(ResultListener.class);
        final ActorThread thread = Mockito.mock(ActorThread.class);
        final ResourceCleaner cleaner = Mockito.mock(ResourceCleaner.class);

        final PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(thread, listener);
        promise.forget();

        promise.deliverResult(null, cleaner);

        Mockito.verifyZeroInteractions(listener);
        Mockito.verifyZeroInteractions(cleaner);
    }
}
