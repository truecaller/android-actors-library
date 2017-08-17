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

import android.support.annotation.NonNull;

public abstract class ProxyFactoryBase implements ProxyFactory {

    protected ProxyFactoryBase() {
    }

    @SuppressWarnings("unchecked")
    @NonNull
    protected <T> T defaultProxy(@NonNull Class<T> cls, @NonNull MessageSender sender) {
        if (ResultListener.class.equals(cls)) {
            return (T) new ResultListenerProxy(sender);
        }

        throw new IllegalArgumentException("Proxy for class " + cls.getCanonicalName() + " was not generated. " +
                "Did you forget to annotate interface by @ActorInterface annotation?");
    }
}
