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

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPowerManager;
import org.robolectric.shadows.ShadowService;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ActorServiceTest {
    private static final String SERVICE_NAME = "test-service";

    @Mock
    private FailureHandler mFailureHandler;

    @Mock
    private Message mMessage;

    private final Object mImpl = new Object();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private ActorService createService() {
        return createService(false);
    }

    private ActorService createService(boolean useWakeLock) {
        ActorService result = new ActorService(SERVICE_NAME, 0, useWakeLock);
        ServiceController<ActorService> serviceController = ServiceController.of(Robolectric.getShadowsAdapter(), result, null);
        serviceController.create();
        return result;
    }

    @Test
    public void onBind_hasValidLocalInterface_always() throws Exception {
        IBinder binder = createService().onBind(new Intent());
        Assert.assertEquals(ActorService.LOCAL_SENDER_INTERFACE, binder.getInterfaceDescriptor());
        Assert.assertNotNull(binder.queryLocalInterface(ActorService.LOCAL_SENDER_INTERFACE));
    }

    @Test
    public void onDestroy_terminateAndClear_always() throws Exception {
        ActorService service = createService();
        ShadowLooper shadowLooper = Shadows.shadowOf(service.mThread.getLooper());
        IBinder binder = service.onBind(new Intent());

        service.onDestroy();

        Assert.assertTrue(shadowLooper.hasQuit());
        Assert.assertNull(null, binder.getInterfaceDescriptor());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deliver_deliverMessage_always() throws Exception {
        ActorService service = createService();
        IBinder binder = service.onBind(new Intent());
        ActorService.RemoteMessageSender sender = (ActorService.RemoteMessageSender) binder.queryLocalInterface(ActorService.LOCAL_SENDER_INTERFACE);

        sender.deliver(Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler));

        ShadowLooper shadowLooper = Shadows.shadowOf(service.mThread.getLooper());
        shadowLooper.runOneTask();

        Mockito.verify(mMessage).invoke(mImpl);
        Assert.assertEquals(false, Shadows.shadowOf(service).isStoppedBySelf());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deliver_useWakeLocks_always() throws Exception {
        ActorService service = createService(true);
        IBinder binder = service.onBind(new Intent());
        ActorService.RemoteMessageSender sender = (ActorService.RemoteMessageSender) binder.queryLocalInterface(ActorService.LOCAL_SENDER_INTERFACE);

        sender.deliver(Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler));

        ShadowLooper shadowLooper = Shadows.shadowOf(service.mThread.getLooper());
        shadowLooper.runOneTask();

        PowerManager.WakeLock wakeLock = ShadowPowerManager.getLatestWakeLock();
        Assert.assertNotNull(wakeLock);
        Assert.assertEquals(false, wakeLock.isHeld());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void deliver_stopService_always() throws Exception {
        ActorService service = createService();
        IBinder binder = service.onBind(new Intent());
        ActorService.RemoteMessageSender sender = (ActorService.RemoteMessageSender) binder.queryLocalInterface(ActorService.LOCAL_SENDER_INTERFACE);

        sender.deliver(Transaction.<Object>obtain(mImpl, mMessage, mFailureHandler));

        ShadowLooper shadowLooper = Shadows.shadowOf(service.mThread.getLooper());

        shadowLooper.runOneTask();
        Assert.assertEquals(false, Shadows.shadowOf(service).isStoppedBySelf());

        shadowLooper.runOneTask();
        Assert.assertEquals(true, Shadows.shadowOf(service).isStoppedBySelf());
    }

    @Test
    public void onStartCommand_START_NOT_STICKY_allways() {
        ActorService service = createService();
        Assert.assertEquals(Service.START_NOT_STICKY, service.onStartCommand(new Intent(), 0, 10));
    }
}
