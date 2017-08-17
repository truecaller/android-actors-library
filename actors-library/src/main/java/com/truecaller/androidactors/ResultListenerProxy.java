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

/* package */ class ResultListenerProxy implements ResultListener {

    private final MessageSender mMessageSender;

    /* package */ ResultListenerProxy(MessageSender messageSender) {
        mMessageSender = messageSender;
    }

    @Override
    public void onResult(@Nullable Object result) {
        mMessageSender.deliver(new ResultListenerMessage(new ActorMethodInvokeException(), result));
    }

    /* package */ static class ResultListenerMessage extends MessageBase<ResultListener, Void> {

        @Nullable
        private final Object mResult;

        /* package */ ResultListenerMessage(@NonNull ActorMethodInvokeException exception, @Nullable Object result) {
            super(exception);
            mResult = result;
        }

        @Nullable
        @Override
        public Promise<Void> invoke(@NonNull ResultListener target) {
            if (target instanceof ExceptionTemplateProvider) {
                mExceptionTemplate = ((ExceptionTemplateProvider) target).exception();
            }
            //noinspection unchecked
            target.onResult(mResult);
            return null;
        }

        @Override
        public String toString() {
            if (mResult instanceof Message) {
                return mResult.toString();
            }
            return ".onResult(" + mResult + ")";
        }
    }
}
