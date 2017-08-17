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

public class TestPromiseTest {

    private final Object mResult = new Object();

    @Mock
    private ActorThread mThread;

    @Mock
    private ResultListener<Object> mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void verifyThenNothing_false_byDefault() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        Assert.assertFalse(promise.verifyThenNothing());
    }

    @Test
    public void verifyThenNothing_true_callThenNothing() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.thenNothing();
        Assert.assertTrue(promise.verifyThenNothing());
    }

    @Test
    public void verifyThenNothing_false_afterReset() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.thenNothing();
        promise.reset();
        Assert.assertFalse(promise.verifyThenNothing());
    }

    @Test
    public void verifyThenNothing_doesNotIntersect_callThenNothing() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.thenNothing();
        Assert.assertFalse(promise.verifyThen());
        Assert.assertFalse(promise.verifyThenWithThread());
        Assert.assertFalse(promise.verifyForget());
        Assert.assertFalse(promise.verifyGet());
    }

    @Test
    public void verifyThen_false_byDefault() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        Assert.assertFalse(promise.verifyThen());
    }

    @Test
    public void verifyThen_true_callThen() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.then(mListener);
        Assert.assertTrue(promise.verifyThen());
    }

    @Test
    public void then_resultIsTheSame_callThen() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.then(mListener);
        Mockito.verify(mListener).onResult(mResult);
    }

    @Test
    public void verifyThen_false_afterReset() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.then(mListener);
        promise.reset();
        Assert.assertFalse(promise.verifyThen());
    }

    @Test
    public void verifyThen_doesNotIntersect_callThen() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.then(mListener);
        Assert.assertFalse(promise.verifyThenNothing());
        Assert.assertFalse(promise.verifyThenWithThread());
        Assert.assertFalse(promise.verifyForget());
        Assert.assertFalse(promise.verifyGet());
    }

    @Test
    public void verifyThenWithThread_false_byDefault() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        Assert.assertFalse(promise.verifyThenWithThread());
    }

    @Test
    public void verifyThenWithThread_true_callThen() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.then(mThread, mListener);
        Assert.assertTrue(promise.verifyThenWithThread());
    }

    @Test
    public void then_resultIsTheSame_callThenWithThread() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.then(mThread, mListener);
        Mockito.verify(mListener).onResult(mResult);
    }

    @Test
    public void verifyThenWithThread_false_afterReset() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.then(mThread, mListener);
        promise.reset();
        Assert.assertFalse(promise.verifyThenWithThread());
    }

    @Test
    public void verifyThenWithThread_doesNotIntersect_callThenWithThread() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.then(mThread, mListener);
        Assert.assertFalse(promise.verifyThen());
        Assert.assertFalse(promise.verifyThenNothing());
        Assert.assertFalse(promise.verifyForget());
        Assert.assertFalse(promise.verifyGet());
    }

    @Test
    public void verifyGet_false_byDefault() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        Assert.assertFalse(promise.verifyGet());
    }

    @Test
    public void verifyGet_true_callGet() throws Exception {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.get();
        Assert.assertTrue(promise.verifyGet());
    }

    @Test
    public void get_resultIsTheSame_callGet() throws Exception {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        Assert.assertSame(mResult, promise.get());
    }

    @Test
    public void verifyGet_false_afterReset() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.then(mThread, mListener);
        promise.reset();
        Assert.assertFalse(promise.verifyGet());
    }

    @Test
    public void verifyGet_doesNotIntersect_callGet() throws Exception {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.get();
        Assert.assertFalse(promise.verifyThen());
        Assert.assertFalse(promise.verifyThenWithThread());
        Assert.assertFalse(promise.verifyThenNothing());
        Assert.assertFalse(promise.verifyForget());
    }

    @Test
    public void testHandle_isNotNull_always() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        Assert.assertNotNull(promise.testHandle());
    }

    @Test
    public void verifyForget_false_byDefault() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        Assert.assertFalse(promise.testHandle().verifyForget());
    }

    @Test
    public void verifyForget_true_callForget() throws Exception {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.testHandle().forget();
        Assert.assertTrue(promise.testHandle().verifyForget());
    }

    @Test
    public void verifyForget_false_afterReset() {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.testHandle().forget();
        promise.reset();
        Assert.assertFalse(promise.testHandle().verifyForget());
    }

    @Test
    public void verifyForget_doesNotIntersect_callForget() throws Exception {
        TestPromise<Object> promise = new TestPromise<>(mResult);
        promise.testHandle().forget();
        Assert.assertFalse(promise.verifyThen());
        Assert.assertFalse(promise.verifyThenWithThread());
        Assert.assertFalse(promise.verifyThenNothing());
        Assert.assertFalse(promise.verifyGet());
    }
}
