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

import android.os.Looper;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DefaultActorThreadTest {
    @Mock
    private ProxyFactory mProxyFactory;

    @Mock
    private FailureHandler mFailureHandler;

    @Mock
    private Runnable mImpl;

    @Mock
    private Runnable mImplProxy;

    @Mock
    private Message mMessage;

    private DefaultActorThread.ThreadFactory mThreadFactory = new DefaultActorThread.ThreadFactory() {
        @Override
        public ActorHandlerBase createThread(Looper looper, long stopDelay) {
            return createHandler(stopDelay, null);
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Mockito.doReturn(mImplProxy).when(mProxyFactory).newProxy(Mockito.eq(Runnable.class), Mockito.<MessageSender>any());
    }

    @NonNull
    private DefaultActorThread createThread() {
        return new DefaultActorThread(mThreadFactory, mProxyFactory, mFailureHandler, "Test Thread", 0);
    }

    @NonNull
    private ActorHandlerBase createHandler(long timeout, @Nullable PowerManager.WakeLock wakeLock) {
        ActorHandlerBase handler = Mockito.mock(ActorHandlerBase.class,
                Mockito.withSettings()
                        .useConstructor(ShadowLooper.getMainLooper(), timeout, wakeLock));
        Mockito.doCallRealMethod().when(handler).obtainMessage(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doCallRealMethod().when(handler).obtainMessage(Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.any());
        return handler;
    }

    @Test
    public void bind_returnValidRef_always() {
        DefaultActorThread thread = createThread();
        ActorRef<Runnable> ref = thread.bind(Runnable.class, mImpl);

        Assert.assertSame(mImplProxy, ref.tell());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void sendTransaction_sendToHandler_validHandlerExists() {
        DefaultActorThread thread = createThread();
        ActorHandlerBase handler = createHandler(-1, null);

        Mockito.doReturn(true).when(handler).sendTransaction(Mockito.<Transaction>any());

        thread.mHandler = handler;

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        thread.sendTransaction(transaction);

        Mockito.verify(handler).sendTransaction(transaction);
        Assert.assertSame(handler, thread.mHandler);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void sendTransaction_sendToHandler_anotherThreadCreatedNewHandler() {
        DefaultActorThread thread = createThread();
        ActorHandlerBase handler = createHandler(-1, null);

        // It will mimic new handler creation
        Mockito.doReturn(false, true).when(handler).sendTransaction(Mockito.<Transaction>any());

        thread.mHandler = handler;

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        thread.sendTransaction(transaction);

        Mockito.verify(handler, Mockito.times(2)).sendTransaction(transaction);
        Assert.assertSame(handler, thread.mHandler);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void sendTransaction_sendToNewHandler_invalidHandlerExists() {
        DefaultActorThread thread = createThread();
        ActorHandlerBase handler = createHandler(-1, null);

        Mockito.doReturn(false).when(handler).sendTransaction(Mockito.<Transaction>any());

        thread.mHandler = handler;

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        thread.sendTransaction(transaction);

        Mockito.verify(thread.mHandler).sendTransaction(transaction);
        Assert.assertNotSame(handler, thread.mHandler);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void sendTransaction_sendToHandler_initialState() {
        DefaultActorThread thread = createThread();

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        thread.sendTransaction(transaction);

        Assert.assertNotNull(thread.mHandler);
        Mockito.verify(thread.mHandler).sendTransaction(transaction);
    }
}
