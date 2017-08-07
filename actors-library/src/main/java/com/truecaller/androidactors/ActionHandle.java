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

/**
 * Handle to the actor call. This handle holds the link to the {@link ResultListener} instance,
 * in case you are not interested in the result any more and want to let GC collect
 * your result listener and the outer classes (if any)
 */
public interface ActionHandle {
    /**
     * Forget the link to the listener.
     * NOTE: It doesn't mean that there is no chance that the listener will not be triggered
     *       after this call. It only prevents the listener and the related classes from leaking
     *       until the background call finishes
     */
    void forget();
}
