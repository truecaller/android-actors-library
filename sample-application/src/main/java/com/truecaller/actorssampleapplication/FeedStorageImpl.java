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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.truecaller.actorssampleapplication.FeedContract.Feed;
import com.truecaller.androidactors.Promise;

/* package */ class FeedStorageImpl implements FeedStorage {

    private static final Uri NOTIFICATION_URI = Uri.parse("content://feed/");

    private final ContentResolver mContentResolver;

    private final SQLiteOpenHelper mHelper;

    /* package */ FeedStorageImpl(ContentResolver contentResolver, @NonNull SQLiteOpenHelper helper) {
        mContentResolver = contentResolver;
        mHelper = helper;
    }

    @Override
    public void save(@Nullable FeedEntry[] entries) {
        if (entries == null) {
            return;
        }

        final SQLiteDatabase db = mHelper.getWritableDatabase();
        final ContentValues values = new ContentValues();

        db.beginTransaction();
        try {
            for (FeedEntry entry : entries) {
                values.put(Feed.COLUMN_ARTICLE_ID, entry.id);
                values.put(Feed.COLUMN_TITLE, entry.title);
                values.put(Feed.COLUMN_URL, entry.link.toString());
                values.put(Feed.COLUMN_PUBLISHED, entry.published);
                db.insert(Feed.TABLE, null, values);
            }
            db.setTransactionSuccessful();
        } catch (RuntimeException e) {
            return;
        } finally {
            db.endTransaction();
        }
        mContentResolver.notifyChange(NOTIFICATION_URI, null);
    }

    @NonNull
    @Override
    public Promise<FeedEntryCursor> fetch() {
        try {
            final SQLiteDatabase db = mHelper.getReadableDatabase();
            @SuppressLint("Recycle")
            Cursor cursor = db.query(Feed.TABLE, null, null, null, null, null, Feed.COLUMN_PUBLISHED + " DESC");
            if (cursor == null) {
                return Promise.wrap(null);
            }
            cursor.setNotificationUri(mContentResolver, NOTIFICATION_URI);

            return Promise.wrap(new FeedEntryCursorImpl(cursor), Cursor::close);
        } catch (Exception exception) {
            return Promise.wrap(null);
        }
    }
}
