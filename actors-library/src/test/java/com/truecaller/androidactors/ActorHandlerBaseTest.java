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

import android.content.Context;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPowerManager;

import java.util.List;

@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ActorHandlerBaseTest {

    @Mock
    private FailureHandler mFailureHandler;

    @Mock
    private Message mMessage;

    @Mock
    private ActorInvokeException mException;

    @Captor
    private ArgumentCaptor<android.os.Message> mMessageCaptor;

    @Captor
    private ArgumentCaptor<Long> mTimeoutCaptor;

    private final Object mImpl = new Object();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private ActorHandlerBase createHandler(long timeout, @Nullable PowerManager.WakeLock wakeLock) {
        ActorHandlerBase handler = Mockito.mock(ActorHandlerBase.class,
                Mockito.withSettings()
                        .useConstructor(ShadowLooper.getMainLooper(), timeout, wakeLock));
        Mockito.doCallRealMethod().when(handler).handleMessage(Mockito.<android.os.Message>any());
        Mockito.doCallRealMethod().when(handler).sendTransaction(Mockito.<Transaction>any());
        Mockito.doCallRealMethod().when(handler).obtainMessage(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt());
        Mockito.doCallRealMethod().when(handler).obtainMessage(Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyInt(), Mockito.any());
        Mockito.doReturn(true).when(handler).sendMessage(Mockito.<android.os.Message>any());

        return handler;
    }

    private ActorHandlerBase createHandler(long timeout) {
        return createHandler(timeout, null);
    }

    @Test
    public void handleMessage_callMethod_methodTransaction() {
        ActorHandlerBase handler = createHandler(ActorHandlerBase.NO_DELAY);

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        android.os.Message message = android.os.Message.obtain(handler, ActorHandlerBase.MSG_TRANSACTION,
                10, 0, transaction);

        handler.handleMessage(message);

        Mockito.verify(mMessage).invoke(mImpl);
        // transaction should always be recycled
        Assert.assertNull(transaction.message);
    }

    @Test
    public void handleMessage_noPoisonPill_methodTransaction() {
        ActorHandlerBase handler = createHandler(ActorHandlerBase.NO_DELAY);

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        android.os.Message message = android.os.Message.obtain(handler, ActorHandlerBase.MSG_TRANSACTION,
                10, 0, transaction);

        handler.handleMessage(message);

        Mockito.verify(handler, Mockito.never()).removeMessages(ActorHandlerBase.MSG_POISON_PILL);
        Mockito.verify(handler, Mockito.never()).sendMessage(Mockito.<android.os.Message>any());
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    public void handleMessage_callFailureHandle_methodTransactionThrows() {
        ActorHandlerBase handler = createHandler(ActorHandlerBase.NO_DELAY);

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        android.os.Message message = android.os.Message.obtain(handler, ActorHandlerBase.MSG_TRANSACTION,
                10, 0, transaction);

        RuntimeException exception = new RuntimeException();
        Mockito.doThrow(exception).when(mMessage).invoke(Mockito.any());
        Mockito.doReturn(mException).when(mMessage).exception();
        handler.handleMessage(message);

        Mockito.verify(mFailureHandler).onUncaughtException(mImpl, mMessage, mException);
        Mockito.verify(mException).initCause(exception);
        // transaction should always be recycled
        Assert.assertNull(transaction.message);
    }

    @Test
    public void handleMessage_takePoisonPill_methodTransaction() {
        ActorHandlerBase handler = createHandler(60000);

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        android.os.Message message = android.os.Message.obtain(handler, ActorHandlerBase.MSG_TRANSACTION,
                10, 0, transaction);

        handler.handleMessage(message);

        Mockito.verify(handler).removeMessages(ActorHandlerBase.MSG_POISON_PILL);
        Mockito.verify(handler).sendMessageDelayed(mMessageCaptor.capture(), mTimeoutCaptor.capture());

        Assert.assertEquals(60000L, (long) mTimeoutCaptor.getValue());

        android.os.Message poisonPill = mMessageCaptor.getValue();
        Assert.assertNotNull(poisonPill);
        Assert.assertEquals(ActorHandlerBase.MSG_POISON_PILL, poisonPill.what);
        Assert.assertEquals(10, poisonPill.arg1);
    }

    @Test
    public void handleMessage_takePoisonPill_methodTransactionThrows() {
        ActorHandlerBase handler = createHandler(60000);

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        android.os.Message message = android.os.Message.obtain(handler, ActorHandlerBase.MSG_TRANSACTION,
                10, 0, transaction);

        RuntimeException exception = new RuntimeException();
        Mockito.doThrow(exception).when(mMessage).invoke(Mockito.any());
        Mockito.doReturn(mException).when(mMessage).exception();
        handler.handleMessage(message);

        Mockito.verify(handler).removeMessages(ActorHandlerBase.MSG_POISON_PILL);
        Mockito.verify(handler).sendMessageDelayed(mMessageCaptor.capture(), mTimeoutCaptor.capture());

        Assert.assertEquals(60000L, (long) mTimeoutCaptor.getValue());

        android.os.Message poisonPill = mMessageCaptor.getValue();
        Assert.assertNotNull(poisonPill);
        Assert.assertEquals(ActorHandlerBase.MSG_POISON_PILL, poisonPill.what);
        Assert.assertEquals(10, poisonPill.arg1);
    }

    @Test
    public void handleMessage_doNothing_outdatedPoisonPill() {
        ActorHandlerBase handler = createHandler(10000);

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        android.os.Message message = android.os.Message.obtain(handler, ActorHandlerBase.MSG_POISON_PILL,
                10, 0, transaction);

        handler.handleMessage(message);

        Mockito.verify(handler, Mockito.never()).stopThread();
    }

    @Test
    public void handleMessage_stopThread_validPoisonPill() {
        ActorHandlerBase handler = createHandler(10000);

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        android.os.Message message = android.os.Message.obtain(handler, ActorHandlerBase.MSG_POISON_PILL,
                0, 0, transaction);

        handler.handleMessage(message);

        Mockito.verify(handler).stopThread();
    }

    @Test
    public void sendTransaction_sendMessage_validHandler() {
        ActorHandlerBase handler = createHandler(ActorHandlerBase.NO_DELAY);
        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);

        boolean result = handler.sendTransaction(transaction);
        Assert.assertTrue(result);

        Mockito.verify(handler).sendMessage(mMessageCaptor.capture());

        android.os.Message message = mMessageCaptor.getValue();
        Assert.assertNotNull(message);

        Assert.assertEquals(ActorHandlerBase.MSG_TRANSACTION, message.what);
        Assert.assertSame(transaction, message.obj);
    }

    @Test
    public void sendTransaction_increaseLastId_validHandler() {
        ActorHandlerBase handler = createHandler(ActorHandlerBase.NO_DELAY);

        Assert.assertTrue(handler.sendTransaction(Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler)));
        Assert.assertTrue(handler.sendTransaction(Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler)));

        Mockito.verify(handler, Mockito.times(2)).sendMessage(mMessageCaptor.capture());

        List<android.os.Message> messages = mMessageCaptor.getAllValues();
        Assert.assertEquals(2, messages.size());

        Assert.assertNotEquals(messages.get(0).arg1, messages.get(1).arg1);
    }

    @Test
    public void sendTransaction_doNothing_stoppedHandler() {
        ActorHandlerBase handler = createHandler(10000);

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        android.os.Message message = android.os.Message.obtain(handler, ActorHandlerBase.MSG_POISON_PILL,
                0, 0, transaction);
        handler.handleMessage(message);

        /**
         *  At this point handler should stop thread. This logic makes test dependent
         *  from {@link #handleMessage_takePoisonPill_methodTransaction()}
         */

        boolean result = handler.sendTransaction(Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler));

        Assert.assertEquals(false, result);
        Mockito.verify(handler, Mockito.never()).sendMessageDelayed(Mockito.<android.os.Message>any(), Mockito.anyLong());
    }

    @Test
    public void handleMessage_wakeLockAcquireRelease_methodTransaction() {
        PowerManager pm = (PowerManager) ShadowApplication.getInstance().getApplicationContext().getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = Shadows.shadowOf(pm).newWakeLock(0, "test-wakelock");
        ActorHandlerBase handler = createHandler(ActorHandlerBase.NO_DELAY, wakeLock);

        Transaction transaction = Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler);
        android.os.Message message = android.os.Message.obtain(handler, ActorHandlerBase.MSG_TRANSACTION,
                10, 0, transaction);

        handler.handleMessage(message);

        Assert.assertSame(wakeLock, ShadowPowerManager.getLatestWakeLock());
        Assert.assertEquals(false, wakeLock.isHeld());
    }
}
