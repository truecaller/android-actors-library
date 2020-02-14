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

import androidx.annotation.NonNull;

/**
 * Special actor thread for using in tests.
 */
public class TestActorThread implements ActorThread {
    /**
     * @param cls actor interface class
     * @param impl implementation of actor interface
     * @param <T> actor interface itself
     * @return ActorRef which points directly to implementation
     */
    @NonNull
    @Override
    public <T> ActorRef<T> bind(@NonNull Class<T> cls, @NonNull T impl) {
        return new TestActorRef<>(impl);
    }

    private static class TestActorRef<T> implements ActorRef<T> {

        @NonNull
        private final T mImpl;

        private TestActorRef(@NonNull T impl) {
            mImpl = impl;
        }

        @Override
        public T tell() {
            return mImpl;
        }
    }
}
