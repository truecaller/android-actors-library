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

import android.content.Intent;
import android.os.IBinder;
import com.truecaller.androidactors.ActorService.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowService;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ActorServiceTest {
    @Mock
    private Object mActorImpl;

    @Mock
    private FailureHandler mFailureHandler;

    private ActorService mService;

    private ShadowService mShadowService;

    private ActorService.ServiceMessageSender mMessageSender;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mService = new ActorService("test");
        ServiceController<ActorService> serviceController = ServiceController.of(Robolectric.getShadowsAdapter(), mService, null);
        serviceController.create();

        mShadowService = Shadows.shadowOf(mService);
        IBinder binder = mService.onBind(new Intent());

        mMessageSender = (ActorService.ServiceMessageSender) binder.queryLocalInterface(ActorService.LOCAL_SENDER_INTERFACE);

        Transaction.clearPool();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void oneMessageDeliveryTest() {
        Assert.assertNotNull(mMessageSender);

        ShadowLooper shadowLooper = Shadows.shadowOf(mService.mThread.getLooper());

        final Message message = Mockito.mock(Message.class);

        Transaction transaction = Mockito.spy(Transaction.obtain(mActorImpl, message, mFailureHandler));
        Assert.assertTrue(mMessageSender.deliver(transaction));
        // We shouldn't do anything directly
        Mockito.verifyZeroInteractions(mActorImpl);

        // Now execute actual task
        shadowLooper.runOneTask();

        Mockito.verify(message).invoke(mActorImpl);
        Mockito.verify(transaction).recycle();

        // Should wait for a while before stopping self
        Assert.assertFalse(mShadowService.isStoppedBySelf());

        // Execute next message. No we should take a poison pill and stop self.
        shadowLooper.runOneTask();
        Assert.assertTrue(mShadowService.isStoppedBySelf());

        // Next execution should reject transaction
        Assert.assertFalse(mMessageSender.deliver(transaction));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void twoMessageDeliveryTest() {
        Assert.assertNotNull(mMessageSender);

        ShadowLooper shadowLooper = Shadows.shadowOf(mService.mThread.getLooper());

        final Message message1 = Mockito.mock(Message.class);
        final Message message2 = Mockito.mock(Message.class);

        Transaction transaction1 = Mockito.spy(Transaction.obtain(mActorImpl, message1, mFailureHandler));
        Transaction transaction2 = Mockito.spy(Transaction.obtain(mActorImpl, message2, mFailureHandler));
        Assert.assertTrue(mMessageSender.deliver(transaction1));
        Assert.assertTrue(mMessageSender.deliver(transaction2));
        // We shouldn't do anything directly
        Mockito.verifyZeroInteractions(mActorImpl);

        // Process first message. Service should not be stopped
        shadowLooper.runOneTask();

        Mockito.verify(message1).invoke(mActorImpl);
        Mockito.verify(transaction1).recycle();

        Assert.assertFalse(mShadowService.isStoppedBySelf());

        // Process second one and check that service was stopped
        shadowLooper.runOneTask();

        Mockito.verify(message1).invoke(mActorImpl);
        Mockito.verify(transaction1).recycle();

        // Should wait for a while before stopping self
        Assert.assertFalse(mShadowService.isStoppedBySelf());

        // Execute next message. No we should take a poison pill and stop self.
        shadowLooper.runOneTask();
        Assert.assertTrue(mShadowService.isStoppedBySelf());
    }

    @SuppressWarnings({"unchecked", "ThrowableNotThrown"})
    @Test
    public void deliverMessageWithExceptionTest() {
        Assert.assertNotNull(mMessageSender);

        ShadowLooper shadowLooper = Shadows.shadowOf(mService.mThread.getLooper());

        final Throwable occurredException = Mockito.mock(RuntimeException.class);
        final Message message = Mockito.mock(Message.class);
        final ActorMethodInvokeException baseCallException = Mockito.mock(ActorMethodInvokeException.class);
        Mockito.doThrow(occurredException).when(message).invoke(Mockito.any());
        Mockito.doReturn(baseCallException).when(message).exception();

        Transaction transaction = Mockito.spy(Transaction.obtain(mActorImpl, message, mFailureHandler));
        Assert.assertTrue(mMessageSender.deliver(transaction));
        // We shouldn't do anything directly
        Mockito.verifyZeroInteractions(mActorImpl);

        // Now execute actual task
        shadowLooper.runOneTask();

        Mockito.verify(message).invoke(mActorImpl);
        Mockito.verify(mFailureHandler).onUncaughtException(mActorImpl, message, baseCallException);
        Mockito.verify(transaction).recycle();
        Mockito.verify(baseCallException).initCause(occurredException);

        // Should wait for a while before stopping self
        Assert.assertFalse(mShadowService.isStoppedBySelf());

        // Execute next message. No we should take a poison pill and stop self.
        shadowLooper.runOneTask();
        Assert.assertTrue(mShadowService.isStoppedBySelf());
    }

    @Test
    public void terminateTest() {
        Assert.assertNotNull(mMessageSender);

        ShadowLooper shadowLooper = Shadows.shadowOf(mService.mThread.getLooper());

        mService.onDestroy();

        Assert.assertTrue(shadowLooper.hasQuit());
    }
}
