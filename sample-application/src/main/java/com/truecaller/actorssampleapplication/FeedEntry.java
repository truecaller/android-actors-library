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

/**
 * This class represents a single entry (post) in the XML feed.
 */
/* package */ class FeedEntry {
    /* package */ final String id;
    /* package */ final String title;
    /* package */ final Uri link;
    /* package */ final long published;

    /* package */ FeedEntry(String id, String title, Uri link, long published) {
        this.id = id;
        this.title = title;
        this.link = link;
        this.published = published;
    }
}
