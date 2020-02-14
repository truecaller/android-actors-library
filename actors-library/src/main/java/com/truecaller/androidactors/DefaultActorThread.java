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

import android.os.HandlerThread;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

public class DefaultActorThread implements ActorThread {

    @NonNull
    private final ThreadFactory mThreadFactory;

    @NonNull
    private final FailureHandler mFailureHandler;

    @NonNull
    private final ProxyFactory mProxyFactory;

    @NonNull
    private final String mThreadName;

    private final long mStopDelay;

    @VisibleForTesting
    @Nullable
    /* package */ ActorHandlerBase mHandler;

    /* package */ DefaultActorThread(@NonNull ProxyFactory proxyFactory, @NonNull FailureHandler failureHandler,
                                     @NonNull String threadName) {
        this(proxyFactory, failureHandler, threadName, ActorHandler.NO_DELAY);
    }

    /* package */ DefaultActorThread(@NonNull ProxyFactory proxyFactory,
                                     @NonNull FailureHandler failureHandler, @NonNull String threadName, long stopDelay) {
        mThreadFactory = new DefaultThreadFactory();
        mProxyFactory = proxyFactory;
        mFailureHandler = failureHandler;
        mThreadName = threadName;
        mStopDelay = stopDelay;
    }

    @VisibleForTesting
    /* package */ DefaultActorThread(@NonNull ThreadFactory threadFactory, @NonNull ProxyFactory proxyFactory,
                                     @NonNull FailureHandler failureHandler, @NonNull String threadName, long stopDelay) {
        mThreadFactory = threadFactory;
        mProxyFactory = proxyFactory;
        mFailureHandler = failureHandler;
        mThreadName = threadName;
        mStopDelay = stopDelay;
    }

    @NonNull
    @Override
    public <T> ActorRef<T> bind(@NonNull Class<T> cls, @NonNull T impl) {
        MessageSenderProxy<T> postman = new MessageSenderProxy<>(mFailureHandler, impl);
        T instance = mProxyFactory.newProxy(cls, postman);
        return new ActorRefImpl<>(instance);
    }

    @VisibleForTesting
    /* package */ void sendTransaction(@NonNull Transaction transaction) {
        ActorHandlerBase handler = mHandler;
        if (handler != null && handler.sendTransaction(transaction)) {
            return;
        }

        synchronized (this) {
            if ((handler = mHandler) != null && handler.sendTransaction(transaction)) {
                return;
            }

            // start thread and schedule transaction
            HandlerThread thread = new HandlerThread(mThreadName);
            thread.start();

            mHandler = mThreadFactory.createThread(thread.getLooper(), mStopDelay);
            mHandler.sendTransaction(transaction);
        }
    }

    private void stopThread(@NonNull Looper looper) {
        ActorHandlerBase handler = mHandler;
        synchronized (this) {
            if (handler == mHandler) {
                mHandler = null;
            }
        }
        looper.quit();
    }

    private class MessageSenderProxy<T> implements MessageSender {

        @NonNull
        private final FailureHandler mFailureHandler;

        @NonNull
        private final T mActorImpl;

        private MessageSenderProxy(@NonNull FailureHandler failureHandler, @NonNull T actorImpl) {
            mActorImpl = actorImpl;
            mFailureHandler = failureHandler;
        }

        @Override
        public void deliver(@NonNull Message message) {
            Transaction transaction = Transaction.<T>obtain(mActorImpl, message, mFailureHandler);
            sendTransaction(transaction);
        }
    }

    private class ActorHandler extends ActorHandlerBase {

        ActorHandler(Looper looper, long stopDelay) {
            super(looper, stopDelay, null);
        }

        @Override
        protected void stopThread() {
            DefaultActorThread.this.stopThread(getLooper());
        }
    }

    private class DefaultThreadFactory implements ThreadFactory {
        @Override
        public ActorHandlerBase createThread(Looper looper, long stopDelay) {
            return new ActorHandler(looper, stopDelay);
        }
    }

    /* package */ interface ThreadFactory {
        ActorHandlerBase createThread(Looper looper, long stopDelay);
    }
}
