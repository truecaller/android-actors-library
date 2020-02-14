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

/* package */ class PromiseProxy<T, R> extends Promise<R> implements Message<T, R>, ActionHandle, ResultListener<R> {
    @NonNull
    private final MessageSender mSender;

    @NonNull
    private final Message<T, R> mMessage;

    @Nullable
    private ActorThread mActorThread = null;

    @Nullable
    private volatile ResultListener<R> mListener = null;

    /* package */ PromiseProxy(@NonNull MessageSender sender, @NonNull Message<T, R> message) {
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
    public void onResult(@Nullable R result) {
        final ResultListener<R> listener = mListener;
        mListener = null;

        if (listener == null) {
            throw new ResultListenerIsNotSpecifiedException();
        }

        listener.onResult(result);
    }

    @Override
    public void forget() {
        mListener = null;
    }

    @Override
    public Promise<R> invoke(@NonNull T impl) {
        Promise<R> result = mMessage.invoke(impl);
        if (result != null) {
            ResultListener<R> listener = mListener;

            if (listener != null && mActorThread != null) {
                result.then(mActorThread, this);
            } else {
                mListener = null;
                //noinspection ConstantConditions
                result.then(listener);
            }
        }
        mActorThread = null;
        return null;
    }

    @NonNull
    @Override
    public ActorInvokeException exception() {
        return mMessage.exception();
    }

    @Override
    public String toString() {
        return mMessage.toString();
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
