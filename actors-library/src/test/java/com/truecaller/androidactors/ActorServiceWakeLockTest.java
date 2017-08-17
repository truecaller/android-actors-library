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
import android.os.PowerManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPowerManager;
import org.robolectric.shadows.ShadowService;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, shadows = {ShadowPowerManager.class})
public class ActorServiceWakeLockTest {
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

        mService = new ActorService("test", 30, true);
        ServiceController<ActorService> serviceController = ServiceController.of(Robolectric.getShadowsAdapter(), mService, null);
        serviceController.create();

        mShadowService = Shadows.shadowOf(mService);
        IBinder binder = mService.onBind(new Intent());

        mMessageSender = (ActorService.ServiceMessageSender) binder.queryLocalInterface(ActorService.LOCAL_SENDER_INTERFACE);

        ActorService.Transaction.clearPool();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void oneMessageDeliveryTest() {
        Assert.assertNotNull(mMessageSender);

        ShadowLooper shadowLooper = Shadows.shadowOf(mService.mThread.getLooper());

        final PowerManager.WakeLock wakeLock = ShadowPowerManager.getLatestWakeLock();

        final Message message = Mockito.mock(Message.class);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Assert.assertSame(wakeLock, ShadowPowerManager.getLatestWakeLock());
                Assert.assertTrue(wakeLock.isHeld());
                return null;
            }
        }).when(message).invoke(Mockito.any());

        ActorService.Transaction transaction = Mockito.spy(ActorService.Transaction.obtain(mActorImpl, message, mFailureHandler));
        Assert.assertTrue(mMessageSender.deliver(transaction));
        // We shouldn't do anything directly
        Mockito.verifyZeroInteractions(mActorImpl);

        // Now execute actual task
        shadowLooper.runOneTask();

        Mockito.verify(message).invoke(mActorImpl);
        Mockito.verify(transaction).recycle();

        // we should release wake lock in any case
        Assert.assertSame(wakeLock, ShadowPowerManager.getLatestWakeLock());
        Assert.assertFalse(wakeLock.isHeld());

        // Should wait for a while before stopping self
        Assert.assertFalse(mShadowService.isStoppedBySelf());

        // Execute next message. Now we should take a poison pill and stop self.
        shadowLooper.runOneTask();
        Assert.assertTrue(mShadowService.isStoppedBySelf());
    }
}
