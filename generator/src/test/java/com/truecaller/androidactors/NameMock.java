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

import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Name;

/* package */ class NameMock implements Name{

    @NotNull
    private final String mValue;

    /* package */ NameMock(@NotNull String value) {
        mValue = value;
    }

    @Override
    public boolean contentEquals(CharSequence cs) {
        return mValue.equals(cs);
    }

    @Override
    public int length() {
        return mValue.length();
    }

    @Override
    public char charAt(int index) {
        return mValue.charAt(0);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return mValue.subSequence(start, end);
    }

    @NotNull
    @Override
    public String toString() {
        return mValue;
    }
}
