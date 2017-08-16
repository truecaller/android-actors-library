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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public class PromiseImplTest {

    private final Object mResult = new Object();

    @Mock
    private ResourceCleaner<Object> mCleaner;

    @Mock
    private ActorThread mActorThread;

    @Mock
    private ResultListener<Object> mListener;

    @Mock
    private ResultListener<Object> mProxy;

    private ActorRef<ResultListener<Object>> mActorRef = new ActorRef<ResultListener<Object>>() {
        @Override
        public ResultListener<Object> tell() {
            return mProxy;
        }
    };

    @Captor
    private ArgumentCaptor<ResultListener<Object>> mCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(mActorRef).when(mActorThread).bind(Mockito.same(ResultListener.class), Mockito.<ResultListener>any());
    }

    @Test
    public void forget_cleanRefs_always() throws Exception {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.forget();

        Mockito.verify(mCleaner).clean(mResult);
    }

    @Test
    public void thenNothing_forgetResult_Always() throws Exception {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.thenNothing();
        // By now it is the only way to get result
        Assert.assertNull(promise.get());
    }

    @Test
    public void then_callListenerOnCurrentThread_withListener() throws Exception {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.then(mListener);
        Mockito.verify(mListener).onResult(mResult);
    }

    @Test
    public void then_cleanResultOnSameThread_withoutThreadAndListener() throws Exception {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.then(null);
        Mockito.verify(mCleaner).clean(mResult);
    }

    @Test
    public void then_doNothing_withoutListenerAndResult() throws Exception {
        PromiseImpl<Object> promise = new PromiseImpl<>(null, mCleaner);
        promise.then(null);
        Mockito.verifyZeroInteractions(mCleaner);
    }

    @Test
    public void then_doNothing_withoutListenerResultAndCleaner() throws Exception {
        PromiseImpl<Object> promise = new PromiseImpl<>(null, null);
        promise.then(null);
        // Just do not crash
    }

    @Test
    public void then_cleanResultOnSameThread_withoutListener() throws Exception {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.then(mActorThread, null);
        Mockito.verifyZeroInteractions(mActorThread);
        Mockito.verify(mCleaner).clean(mResult);
    }

    @Test
    public void then_callListenerOnActorThread_withListener() throws Exception {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.then(mActorThread, mListener);

        Mockito.verify(mActorThread).bind(Mockito.same(ResultListener.class), mCaptor.capture());
        Mockito.verify(mProxy).onResult(mResult);

        mCaptor.getValue().onResult(mResult);
        Mockito.verify(mListener).onResult(mResult);
    }

    @Test
    public void then_cleanResultOnActorThread_withoutListener() throws Exception {
        Mockito.doThrow(new ResultListenerIsNotSpecifiedException()).when(mListener).onResult(Mockito.any());

        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.then(mActorThread, mListener);

        Mockito.verify(mActorThread).bind(Mockito.same(ResultListener.class), mCaptor.capture());
        Mockito.verify(mProxy).onResult(mResult);

        mCaptor.getValue().onResult(mResult);
        Mockito.verify(mCleaner).clean(mResult);
    }

    @Test
    public void then_doNothingOnActorThread_withoutListenerAndResult() throws Exception {
        Mockito.doThrow(new ResultListenerIsNotSpecifiedException()).when(mListener).onResult(Mockito.any());

        PromiseImpl<Object> promise = new PromiseImpl<>(null, mCleaner);
        promise.then(mActorThread, mListener);

        Mockito.verify(mActorThread).bind(Mockito.same(ResultListener.class), mCaptor.capture());
        Mockito.verify(mProxy).onResult(null);

        mCaptor.getValue().onResult(null);
        Mockito.verifyZeroInteractions(mCleaner);
    }

    @Test
    public void ResultListener_cleanOnActorThread_forget() {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        ActionHandle handle = promise.then(mActorThread, mListener);

        Mockito.verify(mActorThread).bind(Mockito.same(ResultListener.class), mCaptor.capture());

        handle.forget();

        Mockito.verify(mProxy).onResult(mResult);

        mCaptor.getValue().onResult(mResult);
        Mockito.verify(mCleaner).clean(mResult);
    }

    @Test
    public void ResultListener_nonNull_exception() {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.then(mActorThread, mListener);

        Mockito.verify(mActorThread).bind(Mockito.same(ResultListener.class), mCaptor.capture());

        ResultListener listener = mCaptor.getValue();
        Assert.assertTrue(listener instanceof ExceptionTemplateProvider);
        Assert.assertNotNull(((ExceptionTemplateProvider) listener).exception());
    }
}
