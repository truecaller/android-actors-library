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

/* package */ class ResultListenerContainerMessage implements Message<ResultListenerContainer> {

    @Nullable
    private final Object mResult;

    @Nullable
    private final ResourceCleaner mCleaner;

    /* package */ ResultListenerContainerMessage(@Nullable Object result, @Nullable ResourceCleaner cleaner) {
        mResult = result;
        mCleaner = cleaner;
    }

    @Override
    public void invoke(ResultListenerContainer target) {
        //noinspection unchecked
        target.deliverResult(mResult, mCleaner);
    }

    @NonNull
    @Override
    public ActorMethodInvokeException exception() {
        return new ActorMethodInvokeException();
    }
}
