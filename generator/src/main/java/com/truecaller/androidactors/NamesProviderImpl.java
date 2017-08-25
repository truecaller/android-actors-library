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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

public class NamesProviderImpl implements NamesProvider {

    @NotNull
    private final Set<CharSequence> mUsed = new TreeSet<>(new NamesComparator());

    @NotNull
    @Override
    public String buildMessageName(@NotNull String methodName) {
        StringBuilder name = new StringBuilder(StringUtils.capitalize(methodName));
        name.append("Message");
        final int length = name.length();
        int index = 0;
        while (mUsed.contains(name)) {
            name.setLength(length);
            name.append(++index);
        }

        String result = name.toString();
        mUsed.add(result);
        return result;
    }

    /* package */ static class NamesComparator implements Comparator<CharSequence> {
        @Override
        public int compare(CharSequence left, CharSequence right) {
            int leftLength = left == null ? -1 : left.length();
            int rightLength = right == null ? -1 : right.length();

            if (leftLength != rightLength) {
                return leftLength - rightLength;
            }

            for (int index = 0; index < leftLength; ++index) {
                int leftC = left.charAt(index);
                int rightC = right.charAt(index);
                int res = leftC - rightC;
                if (res != 0) {
                    return res;
                }
            }

            return 0;
        }
    }
}
