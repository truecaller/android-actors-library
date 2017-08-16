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

public abstract class MessageBase<T, R> implements Message<T, R> {

    @NonNull
    /* package */ ActorInvokeException mExceptionTemplate;

    protected MessageBase(@NonNull ActorInvokeException exception) {
        mExceptionTemplate = exception;
    }

    protected Promise<R> verifyResult(Promise<R> result) {
        if (result == null) {
            AssertionError exception = new AssertionError("Actor methods are not allowed to return null");
            //noinspection UnnecessaryInitCause, have to support Java 1.6
            exception.initCause(mExceptionTemplate);
            throw exception;
        }
        return result;
    }

    @NonNull
    @Override
    public ActorInvokeException exception() {
        return mExceptionTemplate;
    }
}
