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
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.concurrent.Callable;

/**
 * Base Service for all actor threads that are going to be covered by an Android Service.
 * It will manage the background thread for the actor, the service lifecycle
 * and any wake locks if necessary.
 *
 * By default it works as one-shot service (stops itself if there are no calls in
 * the queue) and does not use wake locks. You can change this behaviour by providing
 * constructor parameters.
 *
 * For details read {@link ActorsThreads#createThread(Context, Class, int)}
 *
 * NOTE: This service can't work in remote process
 */
@SuppressLint("Registered")
@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ActorService extends Service {

    /* package */ static final String LOCAL_SENDER_INTERFACE = "ServiceMessageSender";
    /* package */ static final String ACTION_DIRECT_START = "com.truecaller.androidactors.ActorService";

    @NonNull
    private final String mServiceName;

    private final boolean mUseWakelocks;

    private final long mStopDelay;

    @VisibleForTesting
    /* package */ HandlerThread mThread;

    @Nullable
    private Binder mBinder;

    @Nullable
    private ActorJobEngine mJobEngine = null;

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
        // If service was started on pre-Oreo or it was direct service start (not from JobScheduler)
        // we return binder which can be converted to commands executor
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ACTION_DIRECT_START.equals(intent.getAction())) {
            return getLegacyBinder();
        }
        return getBinder();
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

        ServiceMessageSender messageSender = new ServiceMessageSender(new ServiceActorHandler(mThread.getLooper(), mStopDelay, wl));
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

        if (mJobEngine != null) {
            mJobEngine.reportServerStop();
        }
    }

    private IBinder getLegacyBinder() {
        return mBinder;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private IBinder getBinder() {
        ActorJobEngine engine = mJobEngine;
        if (engine == null) {
            engine = JobSchedulerHelper.createJobEngine(this, new Callable<IBinder>() {
                @Override
                public IBinder call() {
                    return mBinder;
                }
            });
        }
        mJobEngine = engine;
        return engine.getBinder();
    }

    /* package */ class ServiceMessageSender implements RemoteMessageSender {
        @NonNull
        private final ActorHandlerBase mHandler;

        /* package */ ServiceMessageSender(@NonNull ActorHandlerBase handler) {
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

    private class ServiceActorHandler extends ActorHandlerBase {
        ServiceActorHandler(Looper looper, long stopDelay, @Nullable PowerManager.WakeLock wakeLock) {
            super(looper, stopDelay, wakeLock);
        }

        @Override
        protected void stopThread() {
            stopSelf();
        }
    }

    /* package */ interface ActorJobEngine {
        void reportServerStop();
        IBinder getBinder();
    }
}
