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

import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.SQLException;
import android.net.Uri;
import com.truecaller.actorssampleapplication.FeedContract.Feed;

/* package */ class FeedEntryCursorImpl extends CursorWrapper implements FeedEntryCursor {

    private final int mId;
    private final int mTitle;
    private final int mUrl;
    private final int mPublish;

    /**
     * Creates a cursor wrapper.
     *
     * @param cursor The underlying cursor to wrap.
     */
    /* package */ FeedEntryCursorImpl(Cursor cursor) {
        super(cursor);
        mId = cursor.getColumnIndexOrThrow(Feed.COLUMN_ARTICLE_ID);
        mTitle = cursor.getColumnIndexOrThrow(Feed.COLUMN_TITLE);
        mUrl = cursor.getColumnIndexOrThrow(Feed.COLUMN_URL);
        mPublish = cursor.getColumnIndexOrThrow(Feed.COLUMN_PUBLISHED);
    }

    @Override
    public FeedEntry getEntry() throws SQLException {
        return new FeedEntry(getString(mId), getString(mTitle), Uri.parse(getString(mUrl)), getLong(mPublish));
    }
}
