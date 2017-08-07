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
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ActorCallLoaderTest {
    @Mock
    private ActorRef<ActorInterface> mActorRef;

    @Mock
    private ActorInterface mActorImpl;

    @Mock
    private ActorThread mUiThread;

    @Mock
    private ActorsThreadsBase mActors;

    @Mock
    private Promise<String> mPromise;

    @Mock
    private ActionHandle mActionHandle;

    private ActorCallLoader<ActorInterface, String> mLoader;

    private ResultCaptor mResultCaptor;

    private CleanupCaptor mCleanupCaptor;

    @Before
    public void prepare() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(mUiThread).when(mActors).ui();
        Mockito.doReturn(mActorImpl).when(mActorRef).tell();

        //noinspection unchecked
        mResultCaptor = new ResultCaptor();
        mCleanupCaptor = new CleanupCaptor();
        mLoader = new ShadowActorCallLoader(mResultCaptor, mCleanupCaptor);
        Mockito.doReturn(mPromise).when(mActorImpl).getResult();
        Mockito.doReturn(mActionHandle).when(mPromise).then(Mockito.<ResultListener<String>>any());
        Mockito.doReturn(mActionHandle).when(mPromise).then(Mockito.<ActorThread>any(), Mockito.<ResultListener<String>>any());
    }

    @Test
    public void onStartLoading() {
        mLoader.onStartLoading();
        // second call should do nothing because previous one is still in progress
        mLoader.onStartLoading();
        Mockito.verify(mActorImpl, Mockito.times(1)).getResult();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void redeliverResult() {
        mLoader.onStartLoading();
        final ArgumentCaptor<ResultListener> captor = ArgumentCaptor.forClass(ResultListener.class);
        Mockito.verify(mPromise).then(Mockito.same(mUiThread), captor.capture());
        ResultListener listener = captor.getValue();
        Assert.assertNotNull(listener);
        listener.onResult("Test");
        // Check that result was delivered
        Assert.assertTrue(mResultCaptor.resultDelivered);
        Assert.assertEquals("Test", mResultCaptor.result);

        // Second call should redeliver result
        Mockito.reset(mActorImpl);
        mResultCaptor.result = null;
        mResultCaptor.resultDelivered = false;
        mLoader.onStartLoading();
        Mockito.verifyZeroInteractions(mActorImpl);
        // but result should be redelivered
        Assert.assertTrue(mResultCaptor.resultDelivered);
        Assert.assertEquals("Test", mResultCaptor.result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onForceLoad() {
        mLoader.onStartLoading();
        Mockito.verify(mActorImpl, Mockito.times(1)).getResult();
        // Result was not delivered, force load should do nothing
        mLoader.onForceLoad();
        Mockito.verify(mActorImpl, Mockito.times(1)).getResult();

        final ArgumentCaptor<ResultListener> captor = ArgumentCaptor.forClass(ResultListener.class);
        Mockito.verify(mPromise).then(Mockito.same(mUiThread), captor.capture());
        ResultListener listener = captor.getValue();
        Assert.assertNotNull(listener);
        listener.onResult("Test");
        // Now force should launch request again
        mLoader.onForceLoad();
        Mockito.verify(mActorImpl, Mockito.times(2)).getResult();
    }

    @Test
    public void onResetWithoutCleanup() {
        mLoader.onStartLoading();
        Mockito.verify(mActorImpl, Mockito.times(1)).getResult();
        mLoader.onReset();
        Assert.assertEquals(false, mCleanupCaptor.cleanUp);
        Mockito.verify(mActionHandle).forget();
        mLoader.onStartLoading();
        Mockito.verify(mActorImpl, Mockito.times(2)).getResult();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onResetWithCleanup() {
        mLoader.onStartLoading();
        Mockito.verify(mActorImpl, Mockito.times(1)).getResult();

        final ArgumentCaptor<ResultListener> captor = ArgumentCaptor.forClass(ResultListener.class);
        Mockito.verify(mPromise).then(Mockito.same(mUiThread), captor.capture());
        ResultListener listener = captor.getValue();
        Assert.assertNotNull(listener);
        listener.onResult("Test");

        mLoader.onReset();
        Assert.assertEquals(true, mCleanupCaptor.cleanUp);
        Assert.assertEquals("Test", mCleanupCaptor.cleanResult);
        Mockito.verify(mActionHandle, Mockito.never()).forget();
        mLoader.onStartLoading();
        Mockito.verify(mActorImpl, Mockito.times(2)).getResult();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void canceledCallWithResult() {
        mLoader.onStartLoading();
        Mockito.verify(mActorImpl, Mockito.times(1)).getResult();
        mLoader.onReset();

        final ArgumentCaptor<ResultListener> captor = ArgumentCaptor.forClass(ResultListener.class);
        Mockito.verify(mPromise).then(Mockito.same(mUiThread), captor.capture());
        ResultListener listener = captor.getValue();
        Assert.assertNotNull(listener);
        listener.onResult("Test");

        Assert.assertEquals(false, mResultCaptor.resultDelivered);
        Assert.assertEquals(true, mCleanupCaptor.cleanUp);
        Assert.assertEquals("Test", mCleanupCaptor.cleanResult);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void canceledCallWithoutResult() {
        mLoader.onStartLoading();
        Mockito.verify(mActorImpl, Mockito.times(1)).getResult();
        mLoader.onReset();

        final ArgumentCaptor<ResultListener> captor = ArgumentCaptor.forClass(ResultListener.class);
        Mockito.verify(mPromise).then(Mockito.same(mUiThread), captor.capture());
        ResultListener listener = captor.getValue();
        Assert.assertNotNull(listener);
        listener.onResult(null);

        Assert.assertEquals(false, mResultCaptor.resultDelivered);
        Assert.assertEquals(false, mCleanupCaptor.cleanUp);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void cachedResultCleanUp() {
        mLoader.onStartLoading();
        Mockito.verify(mActorImpl, Mockito.times(1)).getResult();

        ArgumentCaptor<ResultListener> captor = ArgumentCaptor.forClass(ResultListener.class);
        Mockito.verify(mPromise).then(Mockito.same(mUiThread), captor.capture());
        ResultListener listener = captor.getValue();
        Assert.assertNotNull(listener);
        listener.onResult("Result 1");

        Mockito.reset(mPromise);
        mLoader.onForceLoad();

        captor = ArgumentCaptor.forClass(ResultListener.class);
        Mockito.verify(mPromise).then(Mockito.same(mUiThread), captor.capture());
        listener = captor.getValue();
        Assert.assertNotNull(listener);
        listener.onResult("Result 2");

        Assert.assertEquals(true, mCleanupCaptor.cleanUp);
        Assert.assertEquals("Result 1", mCleanupCaptor.cleanResult);
    }

    private class ShadowActorCallLoader extends ActorCallLoader<ActorInterface, String> {

        private final ResultCaptor mResultCaptor;

        private final CleanupCaptor mCleanupCaptor;

        ShadowActorCallLoader(ResultCaptor resultCaptor, CleanupCaptor cleanupCaptor) {
            super(ShadowApplication.getInstance().getApplicationContext(), mActors, mActorRef);
            mResultCaptor = resultCaptor;
            mCleanupCaptor = cleanupCaptor;
        }

        @Override
        protected Promise<String> doCall(@NonNull ActorRef<ActorInterface> ref) {
            return ref.tell().getResult();
        }

        @Override
        public void deliverResult(String data) {
            mResultCaptor.resultDelivered = true;
            mResultCaptor.result = data;
        }

        @Override
        protected void cleanUp(@NonNull String result) {
            mCleanupCaptor.cleanUp = true;
            mCleanupCaptor.cleanResult = result;
            // call super for marking method as used in JaCoCo report
            super.cleanUp(result);
        }
    }

    private static class ResultCaptor {
        boolean resultDelivered = false;
        String result = null;
    }

    private static class CleanupCaptor {
        boolean cleanUp = false;
        String cleanResult = null;
    }

    interface ActorInterface {
        Promise<String> getResult();
    }
}
