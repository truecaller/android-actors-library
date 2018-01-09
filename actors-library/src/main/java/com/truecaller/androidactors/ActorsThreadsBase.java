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
import android.content.Context;
import android.os.Build;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@SuppressWarnings({"unused", "WeakerAccess", "SameParameterValue"})
public abstract class ActorsThreadsBase implements ActorsThreads {

    @NonNull
    private final ProxyFactory mProxyFactory;

    @NonNull
    private final FailureHandler mFailureHandler;

    @Nullable
    private volatile ActorThread mUiThread = null;

    public ActorsThreadsBase(@NonNull ProxyFactory proxyFactory) {
        this(proxyFactory, new CrashEarlyFailureHandler());
    }

    public ActorsThreadsBase(@NonNull ProxyFactory proxyFactory, @NonNull FailureHandler failureHandler) {
        mProxyFactory = proxyFactory;
        mFailureHandler = failureHandler;
    }

    @Override
    @NonNull
    public ActorThread ui() {
        ActorThread thread = mUiThread;
        if (thread == null) {
            synchronized (mProxyFactory) {
                if ((thread = mUiThread) == null) {
                    thread = createThread(Looper.getMainLooper());
                    mUiThread = thread;
                }
            }
        }
        return thread;
    }

    @Override
    @NonNull
    public ActorThread createThread(@NonNull String name) {
        return new DefaultActorThread(mProxyFactory, mFailureHandler, name);
    }

    @Override
    @NonNull
    public ActorThread createThread(@NonNull String name, long stopDelay) {
        return new DefaultActorThread(mProxyFactory, mFailureHandler, name, stopDelay);
    }

    @Override
    @NonNull
    public ActorThread createThread(@NonNull Looper looper) {
        return new LooperActorThread(mProxyFactory, mFailureHandler, looper);
    }

    @Override
    @NonNull
    public ActorThread createThread(@NonNull Context context, @NonNull Class<? extends ActorService> service, int jobId) {
        return new ServiceActorThread(context, mProxyFactory, mFailureHandler, service, jobId);
    }

    @Override
    @NonNull
    public ActorThread createThread(@NonNull Executor executor) {
        return new ExecutorActorThread(executor,mProxyFactory, mFailureHandler);
    }

    @Override
    @NonNull
    public ActorThread createPooledThread(@NonNull String name, int maxThreads) {
        final Executor executor = new ThreadPoolExecutor(0, maxThreads,5, TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>(), new ActorThreadPoolThreadsFactory(name));
        return new ExecutorActorThread(executor, mProxyFactory, mFailureHandler);
    }
}
