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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.concurrent.Executor;

/* package */ class ExecutorActorThread implements ActorThread {

    @NonNull
    private final Executor mExecutor;

    @NonNull
    private final ProxyFactory mProxyFactory;

    @NonNull
    private final FailureHandler mFailureHandler;

    /* package */ ExecutorActorThread(@NonNull Executor executor, @NonNull ProxyFactory proxyFactory,
                                      @NonNull FailureHandler failureHandler) {
        mExecutor = executor;
        mProxyFactory = proxyFactory;
        mFailureHandler = failureHandler;
    }

    @NonNull
    @Override
    public <T> ActorRef<T> bind(@NonNull Class<T> cls, @NonNull T impl) {
        MessageSenderProxy<T> postman = new MessageSenderProxy<>(impl, mExecutor, mFailureHandler);
        T instance = mProxyFactory.newProxy(cls, postman);
        return new ActorRefImpl<>(instance);
    }

    private static class MessageSenderProxy<T> implements MessageSender {

        @NonNull
        private final Executor mExecutor;

        @NonNull
        private final FailureHandler mFailureHandler;

        @NonNull
        private final T mActorImpl;

        private MessageSenderProxy(@NonNull T actorImpl, @NonNull Executor executor, @NonNull FailureHandler failureHandler) {
            mExecutor = executor;
            mFailureHandler = failureHandler;
            mActorImpl = actorImpl;
        }

        @Override
        public void deliver(@NonNull Message message) {
            Runnable runnable = DeliverRunnable.obtain(mActorImpl, message, mFailureHandler);
            mExecutor.execute(runnable);
        }
    }

    /* package */ static class DeliverRunnable implements Runnable {

        @VisibleForTesting
        /* package */ static final int MAX_POOL_SIZE = 5;

        volatile Object impl;

        volatile Message message;

        volatile FailureHandler failureHandler;

        @VisibleForTesting
        @Nullable
        /* package */ DeliverRunnable next;

        private static DeliverRunnable sTop = null;

        private static int sPoolSize = 0;

        private DeliverRunnable() {
        }

        @VisibleForTesting
        /* package */ void recycle() {
            message = null;
            impl = null;
            failureHandler = null;

            synchronized (DeliverRunnable.class) {
                if (sPoolSize < MAX_POOL_SIZE) {
                    next = sTop;
                    sTop = this;
                    ++sPoolSize;
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            try {
                message.invoke(impl);
            } catch (Throwable e) {
                ActorInvokeException call = message.exception();
                call.initCause(e);
                failureHandler.onUncaughtException(impl, message, call);
            } finally {
                recycle();
            }
        }

        @NonNull
        /* package */ static <T, R> DeliverRunnable obtain(@NonNull T impl, @NonNull Message<T, R> message,
                                                    @NonNull FailureHandler failureHandler) {
            DeliverRunnable runnable = obtain();
            runnable.failureHandler = failureHandler;
            runnable.message = message;
            runnable.impl = impl;
            return runnable;
        }

        @NonNull
        private static DeliverRunnable obtain() {
            synchronized (DeliverRunnable.class) {
                if (sTop != null) {
                    DeliverRunnable result = sTop;
                    sTop = result.next;
                    result.next = null;
                    --sPoolSize;
                    return result;
                }
            }
            return new DeliverRunnable();
        }

        @VisibleForTesting
        /* package */ static void clearPool() {
            synchronized (DeliverRunnable.class) {
                sTop = null;
                sPoolSize = 0;
            }
        }
    }
}
