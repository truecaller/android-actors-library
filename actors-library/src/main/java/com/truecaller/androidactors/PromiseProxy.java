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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/* package */ class PromiseProxy<T, R> extends Promise<R> implements Message<T>, ActionHandle, ResultListenerContainer<R>, Promise.Dispatcher<R> {
    @NonNull
    private final MessageSender mSender;

    @NonNull
    private final MessageWithResult<T> mMessage;

    @Nullable
    private volatile ActorThread mActorThread = null;

    @Nullable
    private volatile ResultListener<R> mListener = null;

    /* package */ PromiseProxy(@NonNull MessageSender sender, @NonNull MessageWithResult<T> message) {
        mSender = sender;
        mMessage = message;
    }

    @Override
    public void thenNothing() {
        mSender.deliver(this);
    }

    @NonNull
    @Override
    public ActionHandle then(@NonNull ResultListener<R> listener) {
        mListener = listener;
        mSender.deliver(this);
        return this;
    }

    @NonNull
    @Override
    public ActionHandle then(@NonNull ActorThread thread, @NonNull ResultListener<R> listener) {
        mActorThread = thread;
        mListener = listener;
        mSender.deliver(this);
        return this;
    }

    @Nullable
    @Override
    public R get() throws InterruptedException {
        BlockResultListener<R> listener = new BlockResultListener<>();
        mListener = listener;
        mSender.deliver(this);
        return listener.waitAndGet();
    }

    @Override
    public void dispatch(@NonNull Promise<R> promise) {
        ActorThread thread = mActorThread;
        mActorThread = null;

        if (thread != null) {
            //noinspection ConstantConditions
            promise.deliver(thread, this);
        } else {
            //noinspection ConstantConditions
            promise.deliver(this);
        }
    }

    @Override
    public void deliverResult(@Nullable R result, @Nullable ResourceCleaner<R> cleaner) {
        final ResultListener<R> listener = mListener;
        mListener = null;

        if (listener == null) {
            if (cleaner != null && result != null) {
                cleaner.clean(result);
            }
            return;
        }

        listener.onResult(result);
    }

    @Override
    public void forget() {
        mListener = null;
    }

    @Override
    public void invoke(T impl) {
        mMessage.invoke(impl, this);
    }

    @NonNull
    @Override
    public ActorMethodInvokeException exception() {
        return mMessage.exception();
    }

    @Override
    public String toString() {
        return mMessage.toString();
    }

    @Override
    void deliver(@NonNull ResultListenerContainer<R> container) {
        throw new AssertionError("Should never be called");
    }

    @Override
    void deliver(@NonNull ActorThread thread, @NonNull ResultListenerContainer<R> container) {
        throw new AssertionError("Should never be called");
    }

    /* package */ static class BlockResultListener<R> implements ResultListener<R> {

        private static final Object EMPTY_INSTANCE = new Object();

        @Nullable
        private volatile Object mResult = EMPTY_INSTANCE;

        @Override
        public void onResult(@Nullable R result) {
            synchronized (this) {
                mResult = result;
                notifyAll();
            }
        }

        /* package */ R waitAndGet() throws InterruptedException {
            Object current;
            synchronized (this) {
                while ((current = mResult) == EMPTY_INSTANCE) {
                    wait();
                }
                mResult = null;
            }

            // It is safe to cast here. We have type safe way of setting this field
            // and empty value was already checked
            @SuppressWarnings("unchecked")
            R result = (R) current;
            return result;
        }
    }
}
