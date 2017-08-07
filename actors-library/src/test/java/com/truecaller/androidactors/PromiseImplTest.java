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


public class PromiseImplTest {

    private final Object mResult = new Object();

    @Mock
    private ResourceCleaner<Object> mCleaner;

    @Mock
    private ActorThread mActorThread;

    @Mock
    private ResultListenerContainer mListenerContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(expected = AssertionError.class)
    public void thenNothingTest() {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.thenNothing();
    }

    @Test(expected = AssertionError.class)
    public void thenSameThreadTest() {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        //noinspection unchecked
        promise.then(Mockito.mock(ResultListener.class));
    }

    @Test(expected = AssertionError.class)
    public void thenActorThreadTest() {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        //noinspection unchecked
        promise.then(mActorThread, Mockito.mock(ResultListener.class));
    }

    @Test
    public void forgetTest() {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        // Should clean result and links
        promise.forget();
        // Check that result was cleaned
        Mockito.verify(mCleaner).clean(mResult);
        // The only way to check that link was cleaned - call forget once again and check cleaner calls
        Mockito.<ResourceCleaner>reset(mCleaner);
        promise.forget();
        Mockito.verifyZeroInteractions(mCleaner);

        // Check that we will not call cleaner with null result
        promise = new PromiseImpl<>(null, mCleaner);
        promise.forget();
        Mockito.verifyZeroInteractions(mCleaner);
    }

    @Test
    public void getTest() throws Exception {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        // First get attempt should clean result
        Assert.assertSame(mResult, promise.get());
        // So second one will return null
        Assert.assertNull(promise.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeliver() {
        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.deliver(mListenerContainer);
        Mockito.verify(mListenerContainer).deliverResult(mResult, mCleaner);
        // Links to result and cleaner should be cleaned, but the only way to get it - call deliver second time
        // it will trigger deliverResult with nulls
        promise.deliver(mListenerContainer);
        Mockito.verify(mListenerContainer).deliverResult(null, null);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDeliverOnThread() {
        ActorRef<ResultListenerContainer> ref = Mockito.mock(ActorRef.class);

        Mockito.doReturn(ref).when(mActorThread).bind(ResultListenerContainer.class, mListenerContainer);
        Mockito.doReturn(mListenerContainer).when(ref).tell();

        PromiseImpl<Object> promise = new PromiseImpl<>(mResult, mCleaner);
        promise.deliver(mActorThread, mListenerContainer);

        Mockito.verify(mListenerContainer).deliverResult(mResult, mCleaner);
        // Links to result and cleaner should be cleaned, but the only way to get it - call deliver second time
        // it will trigger deliverResult with nulls
        promise.deliver(mListenerContainer);
        Mockito.verify(mListenerContainer).deliverResult(null, null);
    }
}
