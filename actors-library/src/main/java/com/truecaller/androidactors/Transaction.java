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
import android.support.annotation.VisibleForTesting;

/* package */  class Transaction {
    @VisibleForTesting
    /* package */ static final int MAX_POOL_SIZE = 5;

    Message message;

    Object impl;

    FailureHandler failureHandler;

    @VisibleForTesting
    @Nullable
    /* package */ Transaction next;

    private static Transaction sTop = null;

    private static int sPoolSize = 0;

    private Transaction() {
    }

    /* package */ void recycle() {
        message = null;
        impl = null;
        failureHandler = null;

        synchronized (Transaction.class) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sTop;
                sTop = this;
                ++sPoolSize;
            }
        }
    }

    @NonNull
    /* package */ static <T> Transaction obtain(@NonNull T impl, @NonNull Message<T, ?> message,
                                                @NonNull FailureHandler failureHandler) {
        Transaction transaction = obtain();
        transaction.impl = impl;
        transaction.message = message;
        transaction.failureHandler = failureHandler;
        return transaction;
    }

    @VisibleForTesting
    /* package */ static void clearPool() {
        synchronized (Transaction.class) {
            sTop = null;
            sPoolSize = 0;
        }
    }

    @NonNull
    private static Transaction obtain() {
        synchronized (Transaction.class) {
            if (sTop != null) {
                Transaction result = sTop;
                sTop = result.next;
                result.next = null;
                --sPoolSize;
                return result;
            }
        }
        return new Transaction();
    }
}
