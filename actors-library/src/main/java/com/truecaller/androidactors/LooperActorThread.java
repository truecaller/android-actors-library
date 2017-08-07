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

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

/* package */ class LooperActorThread implements ActorThread {

    @NonNull
    private final FailureHandler mFailureHandler;

    @NonNull
    private final ProxyFactory mProxyFactory;

    @NonNull
    private final Looper mLooper;

    /* package */ LooperActorThread(@NonNull ProxyFactory proxyFactory, @NonNull FailureHandler failureHandler,
                                    @NonNull Looper looper) {
        mProxyFactory = proxyFactory;
        mFailureHandler = failureHandler;
        mLooper = looper;
    }

    @NonNull
    @Override
    public <T> ActorRef<T> bind(@NonNull Class<T> cls, @NonNull T impl) {
        MessageSender postman = new LooperMessageSender<>(mLooper, mFailureHandler, impl);
        T instance = mProxyFactory.newProxy(cls, postman);
        return new ActorRefImpl<>(instance);
    }

    private static class LooperMessageSender<T> extends Handler implements MessageSender {

        @NonNull
        private final FailureHandler mFailureHandler;

        @NonNull
        private final T mImpl;

        /* package */ LooperMessageSender(@NonNull Looper looper, @NonNull FailureHandler failureHandler,
                                          @NonNull T impl) {
            super(looper);
            mFailureHandler = failureHandler;
            mImpl = impl;
        }

        @Override
        public void deliver(@NonNull Message message) {
            android.os.Message msg = obtainMessage(0, message);
            msg.sendToTarget();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(@NonNull android.os.Message msg) {
            Message<T> message = (Message<T>) msg.obj;
            try {
                message.invoke(mImpl);
            } catch (Throwable e) {
                ActorMethodInvokeException call = message.exception();
                call.initCause(e);
                mFailureHandler.onUncaughtException(mImpl, message, call);
            }
        }
    }
}
