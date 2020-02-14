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

import android.net.Uri;
import com.truecaller.androidactors.Promise;

import androidx.annotation.NonNull;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class NetworkManagerImpl implements NetworkManager {

    private static final FeedEntry EMPTY_RESULT[] = new FeedEntry[0];

    @NonNull
    private final OkHttpClient mHttpClient;

    @NonNull
    private final FeedParser mFeedParser;

    /* package */ NetworkManagerImpl(@NonNull OkHttpClient httpClient, @NonNull FeedParser feedParser) {
        mHttpClient = httpClient;
        mFeedParser = feedParser;
    }

    @NonNull
    @Override
    public Promise<FeedEntry[]> fetch(@NonNull Uri uri) {
        FeedEntry result[] = EMPTY_RESULT;

        final Request request = new Request.Builder().url(uri.toString()).build();
        Response response = null;
        try {
            response = mHttpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                InputStream stream = response.body().byteStream();
                List<FeedEntry> entries = mFeedParser.parse(stream, "UTF-8");
                result = entries.toArray(new FeedEntry[entries.size()]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return Promise.wrap(result);
    }
}
