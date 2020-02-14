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

/**
 * Promise for using in test. Will always provide result on same thread
 *
 * @param <R>
 */
@SuppressWarnings("WeakerAccess")
public class TestPromise<R> extends Promise<R> implements TestActionHandle {

    private static final int ACTION_THEN_NOTHING = 0x01;
    private static final int ACTION_THEN = 0x02;
    private static final int ACTION_THEN_WITH_THREAD = 0x04;
    private static final int ACTION_GET = 0x08;
    private static final int ACTION_FORGET = 0x10;

    private int mAction;

    @Nullable
    private final R mResult;

    /* package */ TestPromise(@Nullable R result) {
        mResult = result;
    }

    @Override
    public void thenNothing() {
        mAction |= ACTION_THEN_NOTHING;
    }

    @NonNull
    @Override
    public ActionHandle then(@NonNull ResultListener<R> listener) {
        mAction |= ACTION_THEN;
        listener.onResult(mResult);
        return this;
    }

    @NonNull
    @Override
    public ActionHandle then(@NonNull ActorThread thread, @NonNull ResultListener<R> listener) {
        mAction |= ACTION_THEN_WITH_THREAD;
        listener.onResult(mResult);
        return this;
    }

    @Nullable
    @Override
    public R get() throws InterruptedException {
        mAction |= ACTION_GET;
        return mResult;
    }

    @Override
    public void forget() {
        mAction |= ACTION_FORGET;
    }

    public TestActionHandle testHandle() {
        return this;
    }

    boolean verifyThenNothing() {
        return hasAction(ACTION_THEN_NOTHING);
    }

    boolean verifyThen() {
        return hasAction(ACTION_THEN);
    }

    boolean verifyThenWithThread() {
        return hasAction(ACTION_THEN_WITH_THREAD);
    }

    boolean verifyGet() {
        return hasAction(ACTION_GET);
    }

    @Override
    public boolean verifyForget() {
        return hasAction(ACTION_FORGET);
    }

    public void reset() {
        mAction = 0;
    }

    private boolean hasAction(int action) {
        return (action & mAction) == action;
    }
}
