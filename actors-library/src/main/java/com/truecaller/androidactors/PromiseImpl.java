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

/* package */  class PromiseImpl<R> extends Promise<R> implements ActionHandle {

    @Nullable
    private R mResult;

    @Nullable
    private ResourceCleaner<R> mCleaner;

    /* package */ PromiseImpl(@Nullable R result, @Nullable ResourceCleaner<R> cleaner) {
        mResult = result;
        mCleaner = cleaner;
    }

    @Override
    public void thenNothing() {
        throw new AssertionError("Should never be called");
    }

    @NonNull
    @Override
    public ActionHandle then(@Nullable ResultListener<R> listener) {
        throw new AssertionError("Should never be called");
    }

    @NonNull
    @Override
    public ActionHandle then(@NonNull ActorThread thread, @Nullable ResultListener<R> listener) {
        throw new AssertionError("Should never be called");
    }

    @Nullable
    @Override
    public R get() throws InterruptedException {
        R result = mResult;
        mResult = null;
        return result;
    }

    @Override
    void deliver(@NonNull ResultListenerContainer<R> container) {
        container.deliverResult(mResult, mCleaner);
        mResult = null;
        mCleaner = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    void deliver(@NonNull ActorThread thread, @NonNull ResultListenerContainer<R> container) {
        ActorRef<ResultListenerContainer> ref = thread.bind(ResultListenerContainer.class, container);
        ref.tell().deliverResult(mResult, mCleaner);
        mResult = null;
        mCleaner = null;
    }

    @Override
    public void forget() {
        cleanUp();
        mCleaner = null;
        mResult = null;
    }

    private void cleanUp() {
        if (mCleaner != null && mResult != null){
            mCleaner.clean(mResult);
        }
    }
}
