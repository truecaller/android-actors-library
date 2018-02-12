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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
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
@SuppressLint("InlinedApi")
@SuppressWarnings("unchecked")
public class ServiceActorThreadTest {

    @Mock
    private ProxyFactory mProxyFactory;

    @Mock
    private FailureHandler mFailureHandler;

    @Mock
    private Runnable mActorImpl;

    @Mock
    private Runnable mProxy;

    @Mock
    private ActorService.RemoteMessageSender mMessageSender;

    private Binder mBinder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Mockito.doReturn(mProxy).when(mProxyFactory).newProxy(Mockito.<Class>any(), Mockito.<MessageSender>any());

        ShadowApplication application = ShadowApplication.getInstance();
        ComponentName name = new ComponentName(application.getApplicationContext(), ActorService.class);
        mBinder = new Binder();
        mBinder.attachInterface(mMessageSender, ActorService.LOCAL_SENDER_INTERFACE);
        Mockito.doReturn(mBinder).when(mMessageSender).asBinder();
        ShadowApplication.getInstance().setComponentNameAndServiceForBindService(name, mBinder);
    }

    @Test
    public void deliverMessageTest() {
        ShadowApplication application = ShadowApplication.getInstance();
        ServiceActorThread thread = new ServiceActorThread(application.getApplicationContext(), mProxyFactory,
                mFailureHandler, ActorService.class, 1);

        ActorRef<Runnable> ref = thread.bind(Runnable.class, mActorImpl);
        Assert.assertSame(mProxy, ref.tell());

        ArgumentCaptor<MessageSender> postmanCaptor = ArgumentCaptor.forClass(MessageSender.class);
        Mockito.verify(mProxyFactory).newProxy(Mockito.<Class>any(), postmanCaptor.capture());

        MessageSender postman = postmanCaptor.getValue();
        Assert.assertNotNull(postman);

        // Should try to start service
        Mockito.doReturn(true).when(mMessageSender).deliver(Mockito.any(Transaction.class));
        Message message = Mockito.mock(Message.class);
        postman.deliver(message);

        Intent intent = application.getNextStartedService();
        Assert.assertNotNull(intent);
        Assert.assertEquals(new ComponentName(application.getApplicationContext(), ActorService.class),
                intent.getComponent());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        Mockito.verify(mMessageSender).deliver(captor.capture());
        Transaction transaction = captor.getValue();

        Assert.assertNotNull(transaction);
        Assert.assertSame(mActorImpl, transaction.impl);
        Assert.assertSame(mFailureHandler, transaction.failureHandler);
        Assert.assertSame(message, transaction.message);

        // Now we bound, but sender will not accept transactions, so we should try to bind again
        Mockito.reset(mMessageSender);
        Mockito.doReturn(mBinder).when(mMessageSender).asBinder();
        Mockito.doReturn(false).when(mMessageSender).deliver(Mockito.any(Transaction.class));
        application.clearStartedServices();
        message = Mockito.mock(Message.class);
        postman.deliver(message);

        intent = application.getNextStartedService();
        Assert.assertNotNull(intent);
        Assert.assertEquals(new ComponentName(application.getApplicationContext(), ActorService.class),
                intent.getComponent());

        captor = ArgumentCaptor.forClass(Transaction.class);
        // Should be two invocation, one when we tried bound one and another after rebinding
        Mockito.verify(mMessageSender, Mockito.times(2)).deliver(captor.capture());
        transaction = captor.getValue();

        Assert.assertNotNull(transaction);
        Assert.assertSame(mActorImpl, transaction.impl);
        Assert.assertSame(mFailureHandler, transaction.failureHandler);
        Assert.assertSame(message, transaction.message);

        // Next one shouldn't try to start service - just do direct call
        Mockito.reset(mMessageSender);
        Mockito.doReturn(mBinder).when(mMessageSender).asBinder();
        Mockito.doReturn(true).when(mMessageSender).deliver(Mockito.<Transaction>any());
        application.clearStartedServices();
        message = Mockito.mock(Message.class);
        postman.deliver(message);
        Assert.assertNull(application.getNextStartedService());

        Mockito.verify(mMessageSender).deliver(captor.capture());
        transaction = captor.getValue();

        Assert.assertNotNull(transaction);
        Assert.assertSame(mActorImpl, transaction.impl);
        Assert.assertSame(mFailureHandler, transaction.failureHandler);
        Assert.assertSame(message, transaction.message);

        // Ok, now unbind and try again. We should start service
        ((ServiceConnection) postman).onServiceDisconnected(null);
        Mockito.reset(mMessageSender);
        Mockito.doReturn(mBinder).when(mMessageSender).asBinder();
        application.clearStartedServices();
        message = Mockito.mock(Message.class);
        postman.deliver(message);

        intent = application.getNextStartedService();
        Assert.assertNotNull(intent);
        Assert.assertEquals(new ComponentName(application.getApplicationContext(), ActorService.class),
                intent.getComponent());

        captor = ArgumentCaptor.forClass(Transaction.class);
        Mockito.verify(mMessageSender).deliver(captor.capture());
        transaction = captor.getValue();

        Assert.assertNotNull(transaction);
        Assert.assertSame(mActorImpl, transaction.impl);
        Assert.assertSame(mFailureHandler, transaction.failureHandler);
        Assert.assertSame(message, transaction.message);
    }

    @Test
    public void emptyInterfaceTest() {
        ShadowApplication application = ShadowApplication.getInstance();
        ComponentName componentName = new ComponentName(application.getApplicationContext(), ActorService.class);
        Binder binder = new Binder();
        ServiceActorThread thread = new ServiceActorThread(application.getApplicationContext(), mProxyFactory,
                mFailureHandler, ActorService.class, 1);

        ActorRef<Runnable> ref = thread.bind(Runnable.class, mActorImpl);
        Assert.assertSame(mProxy, ref.tell());

        ArgumentCaptor<MessageSender> postmanCaptor = ArgumentCaptor.forClass(MessageSender.class);
        Mockito.verify(mProxyFactory).newProxy(Mockito.<Class>any(), postmanCaptor.capture());

        MessageSender postman = postmanCaptor.getValue();
        Assert.assertNotNull(postman);

        // Set null as local interface, it should cause service restart
        ((ServiceConnection) postman).onServiceConnected(componentName, binder);
        // Should attempt to stop service
        Intent intent = application.getNextStoppedService();
        Assert.assertNotNull(intent);
        Assert.assertEquals(new ComponentName(application.getApplicationContext(), ActorService.class),
                intent.getComponent());
        // And start it again
        intent = application.getNextStartedService();
        Assert.assertNotNull(intent);
        Assert.assertEquals(new ComponentName(application.getApplicationContext(), ActorService.class),
                intent.getComponent());

        // Second call should just stop service
        application.clearStartedServices();
        ((ServiceConnection) postman).onServiceConnected(componentName, binder);
        // Should attempt to stop service
        intent = application.getNextStoppedService();
        Assert.assertNotNull(intent);
        Assert.assertEquals(new ComponentName(application.getApplicationContext(), ActorService.class),
                intent.getComponent());

        intent = application.getNextStartedService();
        Assert.assertNull(intent);
    }

    @Test
    public void emptyRemoteExceptionTest() throws Exception {
        ShadowApplication application = ShadowApplication.getInstance();
        ComponentName componentName = new ComponentName(application.getApplicationContext(), ActorService.class);
        IBinder binder = Mockito.mock(IBinder.class);
        Mockito.doThrow(new RemoteException()).when(binder).getInterfaceDescriptor();
        ServiceActorThread thread = new ServiceActorThread(application.getApplicationContext(), mProxyFactory,
                mFailureHandler, ActorService.class, 1);

        ActorRef<Runnable> ref = thread.bind(Runnable.class, mActorImpl);
        Assert.assertSame(mProxy, ref.tell());

        ArgumentCaptor<MessageSender> postmanCaptor = ArgumentCaptor.forClass(MessageSender.class);
        Mockito.verify(mProxyFactory).newProxy(Mockito.<Class>any(), postmanCaptor.capture());

        MessageSender postman = postmanCaptor.getValue();
        Assert.assertNotNull(postman);

        // Set null as local interface, it should cause service restart
        ((ServiceConnection) postman).onServiceConnected(componentName, binder);
        // Should attempt to stop service
        Intent intent = application.getNextStoppedService();
        Assert.assertNotNull(intent);
        Assert.assertEquals(new ComponentName(application.getApplicationContext(), ActorService.class),
                intent.getComponent());
        // And start it again
        intent = application.getNextStartedService();
        Assert.assertNotNull(intent);
        Assert.assertEquals(new ComponentName(application.getApplicationContext(), ActorService.class),
                intent.getComponent());

        // Second call should just stop service
        application.clearStartedServices();
        ((ServiceConnection) postman).onServiceConnected(componentName, binder);
        // Should attempt to stop service
        intent = application.getNextStoppedService();
        Assert.assertNotNull(intent);
        Assert.assertEquals(new ComponentName(application.getApplicationContext(), ActorService.class),
                intent.getComponent());

        intent = application.getNextStartedService();
        Assert.assertNull(intent);
    }
}
