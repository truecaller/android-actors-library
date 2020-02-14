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
 * Wrapper on top of different threads. The only legal way to get
 * actor reference - bind it to one of the actor threads in your app.
 *
 * It is safe to bind different actors implementation instances to the same thread,
 * even if they implement the same interface (or same class). But it is not safe
 * to attach the same instance to different threads (it actually breaks entire idea of actors).
 * Behaviour is undefined if the same instance is attached several times the same actor thread.
 */
public interface ActorThread {
    /**
     * Bind actor implementation to a thread
     *
     * @param cls actor interface class
     * @param impl implementation of actor interface
     * @param <T> actor interface itself
     * @return reference to the actor implementation
     */
    @NonNull
    <T> ActorRef<T> bind(@NonNull Class<T> cls, @NonNull T impl);
}
