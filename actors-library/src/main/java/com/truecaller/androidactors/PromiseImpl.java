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

/* package */  class PromiseImpl<R> extends Promise<R> implements ActionHandle {

    @NonNull
    private final ActorCallbackInvokeException mInvokeException;

    @Nullable
    private volatile R mResult;

    @Nullable
    private ResourceCleaner<R> mCleaner;

    /* package */ PromiseImpl(@Nullable R result, @Nullable ResourceCleaner<R> cleaner) {
        mInvokeException = new ActorCallbackInvokeException();
        mCleaner = cleaner;
        mResult = result;
    }

    @Override
    public void thenNothing() {
        mCleaner = null;
        mResult = null;
    }

    @NonNull
    @Override
    public ActionHandle then(@Nullable ResultListener<R> listener) {
        final R result = mResult;
        final ResourceCleaner<R> cleaner = mCleaner;
        mResult = null;

        if (listener != null) {
            listener.onResult(result);
        } else if (cleaner != null && result != null) {
            cleaner.clean(result);
        }

        mResult = null;
        mCleaner = null;
        return this;
    }

    @NonNull
    @Override
    public ActionHandle then(@NonNull ActorThread thread, @Nullable ResultListener<R> listener) {
        ActionHandle handle = this;

        final ResourceCleaner<R> cleaner = mCleaner;
        final R result = mResult;
        mResult = null;
        mCleaner = null;


        if (listener != null) {
            ResultListenerProxy<R> listenerProxy = new ResultListenerProxy<>(mInvokeException, cleaner, listener);
            //noinspection unchecked
            thread.bind(ResultListener.class, listenerProxy).tell().onResult(result);
            handle = listenerProxy;
        } else if (cleaner != null && result != null) {
            cleaner.clean(result);
        }

        return handle;
    }

    @Nullable
    @Override
    public R get() throws InterruptedException {
        R result = mResult;
        mResult = null;
        return result;
    }

    @Override
    public void forget() {
        final ResourceCleaner<R> cleaner = mCleaner;
        final R result = mResult;

        mResult = null;
        mCleaner = null;

        if (result != null && cleaner != null) {
            cleaner.clean(result);
        }
    }

    private static class ResultListenerProxy<R> implements ResultListener<R>, ActionHandle, ExceptionTemplateProvider {

        @NonNull
        private final ActorCallbackInvokeException mInvokeException;

        private ResultListener<R> mListener;

        private ResourceCleaner<R> mCleaner;

        private ResultListenerProxy(@NonNull ActorCallbackInvokeException invokeException,
                                    @Nullable ResourceCleaner<R> cleaner,
                                    @NonNull ResultListener<R> listener) {
            mInvokeException = invokeException;
            mCleaner = cleaner;
            mListener = listener;
        }

        @Override
        public void onResult(@Nullable R result) {
            if (mListener != null) {
                try {
                    mListener.onResult(result);
                } catch (ResultListenerIsNotSpecifiedException e) {
                    if (mCleaner != null && result != null) {
                        mCleaner.clean(result);
                    }
                }
            } else if (mCleaner != null && result != null) {
                mCleaner.clean(result);
            }

            mCleaner = null;
            mListener = null;
        }

        @Override
        public void forget() {
            mListener = null;
        }

        @NonNull
        @Override
        public ActorInvokeException exception() {
            return mInvokeException;
        }
    }
}
