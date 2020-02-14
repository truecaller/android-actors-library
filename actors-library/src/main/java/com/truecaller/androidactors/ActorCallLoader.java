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

import android.content.Context;
import android.content.Loader;
import android.os.Build;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
@MainThread
public abstract class ActorCallLoader<A, R> extends Loader<R> {

    private static final int STATE_NONE = 0;
    private static final int STATE_CALLING = 1;
    private static final int STATE_DONE = 2;

    @NonNull
    private final ActorsThreads mActorsThreads;

    @NonNull
    private final ActorRef<A> mActorRef;

    private int mState = STATE_NONE;

    @Nullable
    private R mResult;

    @Nullable
    private ActionHandle mActionHandle;

    @Nullable
    private CallResultListener mListener;

    public ActorCallLoader(@NonNull Context context, @NonNull ActorsThreads actors, @NonNull ActorRef<A> actorRef) {
        super(context);
        mActorsThreads = actors;
        mActorRef = actorRef;
    }

    @Override
    protected void onStartLoading() {
        if (mState == STATE_CALLING) {
            // Will call listeners when it will be done
            return;
        }

        if (mState == STATE_DONE) {
            deliverResult(mResult);
            return;
        }

        doLoad();
    }

    @Override
    protected void onForceLoad() {
        if (mState == STATE_CALLING) {
            // Will call listeners when it will be done
            return;
        }

        doLoad();
    }

    @Override
    protected void onReset() {
        mListener = null;
        mState = STATE_NONE;
        if (mActionHandle != null) {
            mActionHandle.forget();
            mActionHandle = null;
        }
        if (mResult != null) {
            cleanUp(mResult);
        }
        mResult = null;
    }

    private void doLoad() {
        mState = STATE_CALLING;
        mListener = new CallResultListener();
        mActionHandle = doCall(mActorRef).then(mActorsThreads.ui(), mListener);
    }

    protected void cleanUp(@NonNull R result) {
    }

    protected abstract Promise<R> doCall(@NonNull ActorRef<A> ref);

    /* package */ class CallResultListener implements ResultListener<R> {
        @Override
        public void onResult(@Nullable R result) {
            if (mListener != this) {
                if (result != null) {
                    cleanUp(result);
                }
                return;
            }

            if (mResult != null) {
                cleanUp(mResult);
            }
            mResult = result;
            mState = STATE_DONE;
            mActionHandle = null;

            deliverResult(result);
        }
    }
}
