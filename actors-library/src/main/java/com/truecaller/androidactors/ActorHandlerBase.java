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

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

/* package */ abstract class ActorHandlerBase extends Handler {

    /* package */ static final long NO_DELAY = -1L;

    @VisibleForTesting
    /* package */ static final int MSG_TRANSACTION = 1;

    @VisibleForTesting
    /* package */ static final int MSG_POISON_PILL = 2;

    @Nullable
    private final PowerManager.WakeLock mWakeLock;

    private final long mStopDelay;

    private volatile int mLastId = 0;

    /* package */ ActorHandlerBase(Looper looper, long stopDelay, @Nullable PowerManager.WakeLock wakeLock) {
        super(looper);
        mWakeLock = wakeLock;
        mStopDelay = stopDelay;
    }

    /* package */ boolean sendTransaction(@NonNull Transaction transaction) {
        final int id;
        synchronized (this) {
            if (mLastId == -1) {
                return false;
            }
            id = ++mLastId;
        }

        return sendMessage(obtainMessage(MSG_TRANSACTION, id, 0, transaction));
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_TRANSACTION:
                handleTransaction((Transaction) msg.obj, msg.arg1);
                break;
            case MSG_POISON_PILL:
                handlePoisonPill(msg.arg1);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleTransaction(@NonNull Transaction transaction, int id) {
        acquireWakelock();
        try {
            transaction.message.invoke(transaction.impl);
        } catch (Throwable e) {
            ActorInvokeException call = transaction.message.exception();
            call.initCause(e);
            transaction.failureHandler.onUncaughtException(transaction.impl, transaction.message, call);
        } finally {
            releaseWakelock();
            transaction.recycle();
        }

        if (mStopDelay != NO_DELAY) {
            removeMessages(MSG_POISON_PILL);
            sendMessageDelayed(obtainMessage(MSG_POISON_PILL, id, 0), mStopDelay);
        }
    }

    private void handlePoisonPill(int id) {
        boolean doStopThread = false;
        synchronized (this) {
            if (mLastId == id) {
                doStopThread = true;
                mLastId = -1;
            }
        }

        if (doStopThread) {
            stopThread();
        }
    }

    private void acquireWakelock() {
        if (mWakeLock == null) {
            return;
        }
        mWakeLock.acquire();
    }

    private void releaseWakelock() {
        if (mWakeLock == null) {
            return;
        }
        mWakeLock.release();
    }

    protected abstract void stopThread();
}
