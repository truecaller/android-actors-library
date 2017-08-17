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
import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.TimeUnit;

/**
 * Base Service for all actor threads that are going to be covered by an Android Service.
 * It will manage the background thread for the actor, the service lifecycle
 * and any wake locks if necessary.
 *
 * By default it works as one-shot service (stops itself if there are no calls in
 * the queue) and does not use wake locks. You can change this behaviour by providing
 * constructor parameters.
 *
 * For details read {@link ActorsThreads#createThread(Context, Class)}
 *
 * NOTE: This service can't work in remote process
 */
@SuppressLint("Registered")
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ActorService extends Service {

    /* package */ static final String LOCAL_SENDER_INTERFACE = "ServiceMessageSender";

    private static final int MSG_TRANSACTION = 1;
    private static final int MSG_POISON_PILL = 2;

    @NonNull
    private final String mServiceName;

    private final boolean mUseWakelocks;

    private final long mStopDelay;

    @VisibleForTesting
    /* package */ HandlerThread mThread;

    @Nullable
    private Binder mBinder;

    /**
     * Service constructor
     *
     * @param name name for the background thread, covered by this service
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    protected ActorService(@NonNull String name) {
        this(name, 0, false);
    }

    /**
     * Service constructor
     *
     * @param name name for the background thread, covered by this service
     * @param stopDelay delay in milliseconds before the service stops itself. In most cases can be 0.
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    protected ActorService(@NonNull String name, long stopDelay) {
        this(name, stopDelay, false);
    }

    /**
     * Service constructor
     *
     * @param name name for the background thread, covered by this service
     * @param stopDelay delay in milliseconds before the service stops itself. In most cases can be 0.
     * @param useWakeLocks true if all calls to the actor implementation should be covered by a partial wake lock
     */
    @SuppressWarnings({"unused", "WeakerAccess"})
    protected ActorService(@NonNull String name, long stopDelay, boolean useWakeLocks) {
        super();
        mServiceName = name;
        mUseWakelocks = useWakeLocks;
        mStopDelay = stopDelay;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mThread = new HandlerThread(mServiceName);
        mThread.start();

        PowerManager.WakeLock wl = null;

        if (mUseWakelocks) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, mServiceName);
            wl.setReferenceCounted(false);
        }

        ServiceMessageSender messageSender = new ServiceMessageSender(new ActorHandler(mThread.getLooper(), mStopDelay, wl));
        mBinder = new Binder();
        mBinder.attachInterface(messageSender, LOCAL_SENDER_INTERFACE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        final Binder binder = mBinder;
        if (binder != null) {
            binder.attachInterface(null, null);
        }
        mThread.quit();
    }

    private class ActorHandler extends Handler {

        @Nullable
        private final PowerManager.WakeLock mWakeLock;

        private final long mStopDelay;

        private volatile int mLastId = 0;

        /* package */ ActorHandler(Looper looper, long stopDelay, @Nullable PowerManager.WakeLock wakeLock) {
            super(looper);
            mWakeLock = wakeLock;
            mStopDelay = stopDelay;
        }

        /* package */ boolean sendTransaction(@NonNull Transaction transaction) {
            final int id;
            synchronized (this) {
                if (mLastId == -1) {
                    return false;
                }
                id = ++mLastId;
            }

            return sendMessage(obtainMessage(MSG_TRANSACTION, id, 0, transaction));
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_TRANSACTION:
                    handleTransaction((Transaction) msg.obj, msg.arg1);
                    break;
                case MSG_POISON_PILL:
                    handlePoisonPill(msg.arg1);
                    break;
            }
        }

        @SuppressWarnings("unchecked")
        private void handleTransaction(@NonNull Transaction transaction, int id) {
            acquireWakelock();
            try {
                try {
                    transaction.message.invoke(transaction.impl);
                } catch (Throwable e) {
                    ActorInvokeException call = transaction.message.exception();
                    call.initCause(e);
                    transaction.failureHandler.onUncaughtException(transaction.impl, transaction.message, call);
                }
            } finally {
                releaseWakelock();
                transaction.recycle();
            }

            removeMessages(MSG_POISON_PILL);
            sendMessageDelayed(obtainMessage(MSG_POISON_PILL, id, 0), mStopDelay);
        }

        private void handlePoisonPill(int id) {
            synchronized (this) {
                if (mLastId == id) {
                    stopSelf();
                    mLastId = -1;
                }
            }
        }

        private void acquireWakelock() {
            if (mWakeLock == null) {
                return;
            }
            mWakeLock.acquire();
        }

        private void releaseWakelock() {
            if (mWakeLock == null) {
                return;
            }
            mWakeLock.release();
        }
    }

    /* package */ class ServiceMessageSender implements RemoteMessageSender {
        @NonNull
        private final ActorHandler mHandler;

        /* package */ ServiceMessageSender(@NonNull ActorHandler handler) {
            mHandler = handler;
        }

        @Override
        public boolean deliver(@NonNull Transaction transaction) {
            return mHandler.sendTransaction(transaction);
        }

        @Override
        public IBinder asBinder() {
            return mBinder;
        }
    }

    /* package */ interface RemoteMessageSender extends IInterface {
        boolean deliver(@NonNull Transaction transaction);
    }

    /* package */ static class Transaction {
        @VisibleForTesting
        /* package */ static final int MAX_POOL_SIZE = 5;

        Message message;

        Object impl;

        FailureHandler failureHandler;

        @VisibleForTesting
        @Nullable
        /* package */ Transaction next;

        private static Transaction sTop = null;

        private static int sPoolSize = 0;

        private Transaction() {
        }

        /* package */ void recycle() {
            message = null;
            impl = null;
            failureHandler = null;

            synchronized (Transaction.class) {
                if (sPoolSize < MAX_POOL_SIZE) {
                    next = sTop;
                    sTop = this;
                    ++sPoolSize;
                }
            }
        }

        @NonNull
        /* package */ static <T> Transaction obtain(@NonNull T impl, @NonNull Message<T, ?> message,
                                                    @NonNull FailureHandler failureHandler) {
            Transaction transaction = obtain();
            transaction.impl = impl;
            transaction.message = message;
            transaction.failureHandler = failureHandler;
            return transaction;
        }

        @VisibleForTesting
        /* package */ static void clearPool() {
            synchronized (Transaction.class) {
                sTop = null;
                sPoolSize = 0;
            }
        }

        @NonNull
        private static Transaction obtain() {
            synchronized (Transaction.class) {
                if (sTop != null) {
                    Transaction result = sTop;
                    sTop = result.next;
                    result.next = null;
                    --sPoolSize;
                    return result;
                }
            }
            return new Transaction();
        }
    }
}
