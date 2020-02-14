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
 * Cleaner for actor call result for cases when no one is waiting for a result
 * @param <T>
 */
public interface ResourceCleaner<T> {
    /**
     * This method will be triggered if the actor finishes its job and passes
     * data to the promise, but no one is waiting for it.
     * @param resource object that should be cleaned. If actor returns null this method will not be called
     */
    void clean(@NonNull T resource);
}
