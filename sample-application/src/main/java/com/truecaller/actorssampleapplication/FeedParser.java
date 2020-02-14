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
import android.text.TextUtils;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/* package */ class FeedParser {

    private enum EntityState {
        Start,
        None,
        Id,
        Published,
        Title,
        End
    }

    private static final String TAG_FEED = "feed";
    private static final String TAG_ENTRY = "entry";
    private static final String TAG_ID = "id";
    private static final String TAG_PUBLISHED = "published";
    private static final String TAG_TITLE = "title";
    private static final String TAG_LINK = "link";

    // 2017-08-03T10:00:00.000-07:00
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'000'ZZZZZ", Locale.ENGLISH);

    @NonNull
    List<FeedEntry> parse(@NonNull InputStream in, @SuppressWarnings("SameParameterValue") @NonNull String charset) {
        final List<FeedEntry> result = new ArrayList<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, charset);
            if (parser.nextTag() == XmlPullParser.START_TAG) {
                readFeed(parser, result);
            }

        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void readFeed(@NonNull XmlPullParser parser, @NonNull List<FeedEntry> result) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, TAG_FEED);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if (TAG_ENTRY.equals(parser.getName())) {
                final FeedEntry entry = readEntry(parser);
                if (entry != null) {
                    result.add(entry);
                }
            } else {
                skip(parser);
            }
        }
    }

    @Nullable
    private FeedEntry readEntry(@NonNull XmlPullParser parser) throws IOException, XmlPullParserException {
        FeedEntry result = null;
        parser.require(XmlPullParser.START_TAG, null, TAG_ENTRY);
        EntityState state = EntityState.Start;
        StringBuilder text = new StringBuilder();

        String id = null;
        String title = null;
        Uri uri = null;
        long published = -1;

        while (state != EntityState.End) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG: {
                    String name = parser.getName();
                    if (TAG_ID.equals(name)) {
                        state = EntityState.Id;
                        text.setLength(0);
                    } else if (TAG_TITLE.equals(name)) {
                        state = EntityState.Title;
                        text.setLength(0);
                    } else if (TAG_LINK.equals(name)) {
                        String rel = parser.getAttributeValue(null, "rel");
                        if ("alternate".equals(rel)) {
                            String rawLink = parser.getAttributeValue(null, "href");
                            if (!TextUtils.isEmpty(rawLink)) {
                                uri = Uri.parse(rawLink);
                                if (!uri.isAbsolute()) {
                                    uri = null;
                                }
                            }
                        }
                    } else if (TAG_PUBLISHED.equals(name)) {
                        state = EntityState.Published;
                        text.setLength(0);
                    }
                    break;
                }
                case XmlPullParser.END_TAG: {
                    String name = parser.getName();
                    if (TAG_ENTRY.equals(name)) {
                        state = EntityState.End;
                    } else if (TAG_ID.equals(name)) {
                        id = text.toString();
                        state = EntityState.None;
                    } else if (TAG_TITLE.equals(name)) {
                        title = text.toString();
                        state = EntityState.None;
                    } else if (TAG_PUBLISHED.equals(name)) {
                        String time = text.toString();
                        if (!TextUtils.isEmpty(time)) {
                            try {
                                Date date = mDateFormat.parse(time);
                                published = date.getTime();
                            } catch (ParseException e) {
                                // Nothing
                            }
                        }
                        state = EntityState.None;
                    }
                    break;
                }
                case XmlPullParser.TEXT: {
                    if (state != EntityState.None) {
                        text.append(parser.getText());
                    }
                    break;
                }
            }
        }

        if (!TextUtils.isEmpty(id) && !TextUtils.isEmpty(title) && uri != null) {
            result = new FeedEntry(id, title, uri, published);
        }

        return result;
    }

    private void skip(@NonNull XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
