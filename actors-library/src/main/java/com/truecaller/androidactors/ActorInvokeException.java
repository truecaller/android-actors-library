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

/* package */ class ActorInvokeException extends RuntimeException {

    @NonNull
    private final String mCallDescription;

    @Nullable
    private String mMessage = null;

    /* package */ ActorInvokeException(@NonNull String callDescription) {
        mCallDescription = callDescription;
    }

    /* package */ void setMethodSignature(@NonNull Class cls, @NonNull Message<?,?> message) {
        mMessage = mCallDescription + " " + cls.getSimpleName() + message;
    }

    @Override
    public String getMessage() {
        if (mMessage != null) {
            return mMessage;
        }

        return super.getMessage();
    }
}
