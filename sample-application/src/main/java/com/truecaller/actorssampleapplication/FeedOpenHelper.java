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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.truecaller.actorssampleapplication.FeedContract.Feed;

/* package */ class FeedOpenHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "feed.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_FEED_CREATE = "CREATE TABLE " + Feed.TABLE + " (" +
            Feed.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            Feed.COLUMN_ARTICLE_ID + " TEXT UNIQUE ON CONFLICT REPLACE, " +
            Feed.COLUMN_TITLE + " TEXT, " +
            Feed.COLUMN_URL + " TEXT, " +
            Feed.COLUMN_PUBLISHED + " INTEGER" +
    ")";

    /* package */ FeedOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_FEED_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
