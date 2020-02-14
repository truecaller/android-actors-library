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

/* package */ interface ExceptionTemplateProvider {
    /**
     * Return prepared exception object which already has knowledge from what actor method it was invoked.
     * Can be useful for debug purposes.
     * @return prepared exception object
     */
    @NonNull
    ActorInvokeException exception();
}
