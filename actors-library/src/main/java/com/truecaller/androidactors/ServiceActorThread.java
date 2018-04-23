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

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import com.truecaller.androidactors.ActorService.RemoteMessageSender;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.Queue;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
/* package */ class ServiceActorThread implements ActorThread {

    private static final SparseArray<WeakReference<ServiceConnection>> sSenders = new SparseArray<>();

    private final int mJobId;

    @NonNull
    private final Context mContext;

    @NonNull
    private final ProxyFactory mProxyFactory;

    @NonNull
    private final FailureHandler mFailureHandler;

    @NonNull
    private final Class<? extends ActorService> mService;

    /* package */ ServiceActorThread(@NonNull Context context, @NonNull ProxyFactory proxyFactory,
                                     @NonNull FailureHandler failureHandler, @NonNull Class<? extends ActorService> service,
                                     int jobId) {
        mContext = context.getApplicationContext();
        mProxyFactory = proxyFactory;
        mFailureHandler = failureHandler;
        mService = service;
        mJobId = jobId;
    }

    @NonNull
    @Override
    public <T> ActorRef<T> bind(@NonNull Class<T> cls, @NonNull T impl) {
        ServiceMessageSenderProxy<T> postman = new ServiceMessageSenderProxy<>(mContext, mFailureHandler, mService, mJobId, impl);
        T instance = mProxyFactory.newProxy(cls, postman);
        return new ActorRefImpl<>(instance);
    }

    @Nullable
    /* package */ static ServiceConnection getConnectionForJob(int jobId) {
        WeakReference<ServiceConnection> ref = sSenders.get(jobId);
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private static class ServiceMessageSenderProxy<T> implements MessageSender, ServiceConnection {

        private final int mJobId;

        @NonNull
        private final Context mContext;

        @NonNull
        private final Intent mIntent;

        @NonNull
        private final T mActorImpl;

        @NonNull
        private final FailureHandler mFailureHandler;

        @Nullable
        private RemoteMessageSender mSender;

        private boolean mRestartAttempt;

        private final Queue<Transaction> mTransactionsQueue = new ArrayDeque<>();

        private boolean mServiceBound;

        private ServiceMessageSenderProxy(@NonNull Context context, @NonNull FailureHandler failureHandler,
                                          @NonNull Class<? extends ActorService> service, int jobId,
                                          @NonNull T actorImpl) {
            mContext = context;
            mFailureHandler = failureHandler;
            mIntent = new Intent(mContext, service);
            mIntent.setAction(ActorService.ACTION_DIRECT_START);
            mJobId = jobId;
            mActorImpl = actorImpl;
        }

        @Override
        public void deliver(@NonNull Message message) {
            Transaction transaction = Transaction.<T>obtain(mActorImpl, message, mFailureHandler);
            final RemoteMessageSender sender;

            synchronized (this) {
                sender = mSender;
            }

            if (sender == null) {
                mTransactionsQueue.add(transaction);
                startService();
                return;
            }

            if (sender.asBinder().isBinderAlive() && sender.deliver(transaction)) {
                return;
            }

            mTransactionsQueue.add(transaction);
            stopActorService();
            startService();
        }

        @Override
        public synchronized void onServiceConnected(ComponentName name, IBinder service) {
            RemoteMessageSender sender = getMessageSender(service);
            if (sender == null) {
                stopActorService();
                if (!mRestartAttempt) {
                    startService();
                    mRestartAttempt = true;
                }
                return;
            }
            // Deliver queued messages
            Transaction transaction;
            while ((transaction = mTransactionsQueue.poll()) != null) {
                sender.deliver(transaction);
            }

            mSender = sender;
            mRestartAttempt = false;
        }

        @Override
        public synchronized void onServiceDisconnected(ComponentName name) {
            mSender = null;
            mServiceBound = false;
        }

        private void startService() {
            try {
                mContext.startService(mIntent);
                mServiceBound = mContext.bindService(mIntent, this, Context.BIND_IMPORTANT);
            } catch (IllegalStateException e) {
                mServiceBound = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sSenders.put(mJobId, new WeakReference<ServiceConnection>(this));
                    JobSchedulerHelper.scheduleJob(mContext, mJobId, mIntent.getComponent());
                }
            }
        }

        private synchronized void stopActorService() {
            if (mServiceBound) {
                mContext.unbindService(this);
            }
            mContext.stopService(mIntent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                JobSchedulerHelper.cancelJob(mContext, mJobId);
            }
            mSender = null;
        }

        @Nullable
        private RemoteMessageSender getMessageSender(IBinder binder) {
            try {
                if (!ActorService.LOCAL_SENDER_INTERFACE.equals(binder.getInterfaceDescriptor())) {
                    return null;
                }

                return (RemoteMessageSender) binder.queryLocalInterface(ActorService.LOCAL_SENDER_INTERFACE);
            } catch (RemoteException e) {
                return null;
            }
        }
    }
}
