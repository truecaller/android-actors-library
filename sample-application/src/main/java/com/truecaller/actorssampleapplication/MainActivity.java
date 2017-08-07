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

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.truecaller.androidactors.ActorCallLoader;
import com.truecaller.androidactors.ActorRef;
import com.truecaller.androidactors.ActorsThreads;
import com.truecaller.androidactors.Promise;
import com.truecaller.androidactors.ResultListener;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<FeedEntryCursor> {

    private static final Uri BLOG_URI = Uri.parse("https://android-developers.googleblog.com/feeds/posts/default");

    @Inject
    /* package */ ActorRef<NetworkManager> mNetworkManager;

    @Inject
    /* package */ ActorRef<FeedStorage> mFeedStorage;

    @Inject
    /* package */ ActorsThreads mActors;

    private ListView mFeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SampleApplication.graph().inject(this);

        setContentView(R.layout.activity_main);

        findViewById(R.id.do_request_button).setOnClickListener(this::onRefreshClicked);
        mFeed = (ListView) findViewById(R.id.feeds);

        getLoaderManager().initLoader(R.id.loader_cursor, null, this);
    }

    private void onRefreshClicked(View v) {
        mNetworkManager.tell().fetch(BLOG_URI).then(e -> mFeedStorage.tell().save(e));
    }

    @Override
    public Loader<FeedEntryCursor> onCreateLoader(int id, Bundle args) {
        if (id == R.id.loader_cursor) {
            return new FeedCursorLoader(this, mActors, mFeedStorage);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<FeedEntryCursor> loader, FeedEntryCursor cursor) {
        mFeed.setAdapter(new FeedAdapter(this, cursor));
    }

    @Override
    public void onLoaderReset(Loader<FeedEntryCursor> loader) {
        mFeed.setAdapter(null);
    }

    private static class FeedAdapter extends CursorAdapter {

        private FeedAdapter(Context context, Cursor c) {
            // Not fair and do not do so in production code
            // Re-query from cursor will happen on UI thread
            super(context, c, true);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView text = (TextView) view.findViewById(android.R.id.text1);
            FeedEntry entry =  ((FeedEntryCursor) cursor).getEntry();
            text.setText(entry.title);
        }
    }

    private static class FeedCursorLoader extends ActorCallLoader<FeedStorage, FeedEntryCursor> {
        /* package */ FeedCursorLoader(@NonNull Context context, @NonNull ActorsThreads actors, @NonNull ActorRef<FeedStorage> actorRef) {
            super(context, actors, actorRef);
        }

        @Override
        protected Promise<FeedEntryCursor> doCall(@NonNull ActorRef<FeedStorage> ref) {
            return ref.tell().fetch();
        }

        @Override
        protected void cleanUp(@NonNull FeedEntryCursor result) {
            if (!result.isClosed()) {
                result.close();
            }
        }
    }
}
