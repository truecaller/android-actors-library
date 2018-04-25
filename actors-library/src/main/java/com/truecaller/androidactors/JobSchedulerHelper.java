/*
 * Copyright (C) 2018 True Software Scandinavia AB
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

import android.annotation.TargetApi;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobServiceEngine;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;

@TargetApi(Build.VERSION_CODES.O)
public abstract class JobSchedulerHelper {
    private JobSchedulerHelper() {}

    public static void scheduleJob(@NonNull Context context, int id, @NonNull ComponentName component) {
        JobInfo job = new JobInfo.Builder(id, component)
                .setOverrideDeadline(0)
                .setMinimumLatency(0)
                .build();
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(job);
    }

    public static void cancelJob(@NonNull Context context, int id) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            scheduler.cancel(id);
        }
    }

    @NonNull
    public static ActorService.ActorJobEngine createJobEngine(@NonNull Service service, @NonNull Callable<IBinder> binder) {
        return new ActorJobServiceEngine(service, binder);
    }

    private static class ActorJobServiceEngine extends JobServiceEngine implements ActorService.ActorJobEngine {
        @NonNull
        private final ComponentName mSelfName;

        @NonNull
        private final WeakReference<Callable<IBinder>> mBinderRef;

        @Nullable
        private JobParameters mLastParams = null;

        private ActorJobServiceEngine(@NonNull Service service, @NonNull Callable<IBinder> binder) {
            super(service);
            mSelfName = new ComponentName(service.getApplicationContext(), service.getClass());
            mBinderRef = new WeakReference<>(binder);
        }

        @Override
        public boolean onStartJob(JobParameters params) {
            ServiceConnection connection = ServiceActorThread.getConnectionForJob(params.getJobId());
            Callable<IBinder> callable = mBinderRef.get();
            if (connection == null || callable == null) {
                return false;
            }

            final IBinder binder;
            try {
                binder = callable.call();
            } catch (Exception e) {
                throw new RuntimeException("Can't fetch binder", e);
            }

            if (binder == null) {
                return false;
            }

            mLastParams = params;
            connection.onServiceConnected(mSelfName, binder);
            return true;
        }

        @Override
        public boolean onStopJob(JobParameters params) {
            return false;
        }

        @Override
        public void reportServerStop() {
            JobParameters params = mLastParams;
            if (params != null) {
                jobFinished(params, false);
            }
        }
    }
}
