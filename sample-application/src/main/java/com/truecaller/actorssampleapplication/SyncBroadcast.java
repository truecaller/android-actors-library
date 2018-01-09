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

package com.truecaller.actorssampleapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.truecaller.androidactors.ActorRef;

import javax.inject.Inject;

public class SyncBroadcast extends BroadcastReceiver {

    @Inject
    /* package */ ActorRef<NetworkManager> mNetworkManager;

    @Inject
    /* package */ ActorRef<FeedStorage> mFeedStorage;

    @Override
    public void onReceive(Context context, Intent intent) {
        SampleApplication.graph().inject(this);

        mNetworkManager.tell().fetch(BuildConfig.BLOG_URI).then(e -> mFeedStorage.tell().save(e));
    }
}
