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

    private Object mImpl = new Object();

    @Mock
    private MessageSender mSender;

    @Mock
    private Message<Object, Object> mMessage;

    @Mock
    private ActorThread mThread;

    @Mock
    private ResultListener<Object> mListener;

    @Mock
    private Promise<Object> mResultPromise;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void toString_returnFromMessage_always() {
        Mockito.doReturn("TEXT FROM MESSAGE").when(mMessage).toString();
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        Assert.assertEquals("TEXT FROM MESSAGE", promise.toString());
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    public void exception_returnFromMessage_always() {
        final ActorInvokeException exception = Mockito.mock(ActorInvokeException.class);
        Mockito.doReturn(exception).when(mMessage).exception();
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        Assert.assertSame(exception, promise.exception());
    }

    @Test
    public void thenNothing_deliverSelf_always() throws Exception {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.thenNothing();

        Mockito.verify(mSender).deliver(promise);
    }

    @Test
    public void then_deliverSelf_withListener() throws Exception {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(mListener);

        Mockito.verify(mSender).deliver(promise);
    }

    @Test
    public void then_deliverSelf_withListenerAndThread() throws Exception {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(mThread, mListener);

        Mockito.verify(mSender).deliver(promise);
    }

    @Test(timeout = 2000)
    public void get_deliverResult_nonNullResult() throws Exception {
        final Object result = new Object();
        final CountDownLatch latch = new CountDownLatch(1);
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        ResultDeliveryThread thread = new ResultDeliveryThread(promise, latch, null);
        Mockito.doAnswer(thread).when(mSender).deliver(Mockito.<Message>any());

        thread.start();
        latch.await();

        promise.onResult(result);
        thread.join();

        Assert.assertSame(result, thread.delivered());
    }

    @Test(timeout = 2000)
    public void get_deliverResult_nullResult() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        ResultDeliveryThread thread = new ResultDeliveryThread(promise, latch, new Object());
        Mockito.doAnswer(thread).when(mSender).deliver(Mockito.<Message>any());

        thread.start();
        latch.await();

        // Allow thread to fall a sleep
        Thread.sleep(200);
        promise.onResult(null);
        thread.join();

        Assert.assertNull(thread.delivered());
    }

    @Test(expected = ResultListenerIsNotSpecifiedException.class)
    public void onResult_throw_withoutListener() {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.onResult(new Object());
    }

    @Test
    public void onResult_provideResult_withListener() {
        final Object result = new Object();
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(mListener);
        promise.onResult(result);

        Mockito.verify(mListener).onResult(result);
    }

    @Test(expected = ResultListenerIsNotSpecifiedException.class)
    public void forget_clearListener_always() {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(mListener);

        promise.forget();
        promise.onResult(new Object());
    }

    @Test
    public void invoke_invokeMessage_withoutResult() {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);

        promise.invoke(mImpl);

        Mockito.verify(mMessage).invoke(mImpl);
    }

    @Test
    public void invoke_invokeMessage_withoutListener() {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        Mockito.doReturn(mResultPromise).when(mMessage).invoke(mImpl);

        promise.invoke(mImpl);

        Mockito.verify(mResultPromise).then(null);
    }

    @Test
    public void invoke_invokeMessageAndCallListener_withoutActorThread() {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(mListener);
        Mockito.doReturn(mResultPromise).when(mMessage).invoke(mImpl);

        promise.invoke(mImpl);

        Mockito.verify(mResultPromise).then(mListener);
    }

    @Test
    public void invoke_invokeMessageAndCallListener_withActorThread() {
        PromiseProxy promise = new PromiseProxy(mSender, mMessage);
        promise.then(mThread, mListener);
        Mockito.doReturn(mResultPromise).when(mMessage).invoke(mImpl);

        promise.invoke(mImpl);

        Mockito.verify(mResultPromise).then(mThread, promise);
    }

    private static class ResultDeliveryThread<T, R> extends Thread implements Answer<Void> {

        @NonNull
        private final PromiseProxy<T, R> mPromise;

        @NonNull
        private final CountDownLatch mStartCountdown;

        private R mDeliveredResult = null;

        private ResultDeliveryThread(@NonNull PromiseProxy<T, R> promise, @NonNull CountDownLatch startCountdown, R initial) {
            mPromise = promise;
            mStartCountdown = startCountdown;
            mDeliveredResult = initial;
        }

        @Override
        public void run() {
            try {
                mDeliveredResult = mPromise.get();
            } catch (InterruptedException e) {
                // nothing here
            }
        }

        R delivered() {
            return mDeliveredResult;
        }

        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            mStartCountdown.countDown();
            return null;
        }
    }
}
