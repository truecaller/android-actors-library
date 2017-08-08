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
import android.support.annotation.Nullable;

/**
 * Promise of the result from an actor's method. It is the only legal way to return
 * something from an actor. Keep in mind that the method itself will not be invoked until you
 * provide what should the library do with the result (call one of the then() methods)
 *
 * @param <R> result type
 */
public abstract class Promise<R> {
    /**
     * Caller does not care about result
     */
    public abstract void thenNothing();

    /**
     * Ask the library to pass a result to the result listener on actor's thread
     *
     * @param listener Class which will receive the call result
     * @return Handle to the call. It will hold an instance to the listener and its context.
     *         Do not forget to call {@link ActionHandle#forget()} when you don't want to
     *         process the result anymore
     */
    @NonNull
    public abstract ActionHandle then(@NonNull ResultListener<R> listener);

    /* package */ abstract void deliver(@NonNull ResultListenerContainer<R> container);
    /**
     * Ask the library to pass a result to the result listener on the provided thread
     *
     * @param thread Actor thread on which the listener will be triggered
     * @param listener Class which will receive the call result
     * @return Handle to the call. It will hold an instance to the listener and its context.
     *         Do not forget to call {@link ActionHandle#forget()} when you don't want to
     *         process the result anymore
     */
    @NonNull
    public abstract ActionHandle then(@NonNull ActorThread thread, @NonNull ResultListener<R> listener);

    /* package */ abstract void deliver(@NonNull ActorThread thread, @NonNull ResultListenerContainer<R> container);
    /**
     * Block the current thread until the actor's method returns a result.
     * In most cases you don't need this method.
     * But if do need it, use it carefully - it can be a reason for deadlocks.
     *
     * @return Actor's method call result
     * @throws InterruptedException
     */
    @Nullable
    public abstract R get() throws InterruptedException;

    /**
     * Wrap a result into a proxy.
     *
     * @param result method result
     * @param <R> result type
     * @return Proxy which can be returned from the actor methods implementations
     */
    @NonNull
    public static <R> Promise<R> wrap(@Nullable R result) {
        return new PromiseImpl<>(result, null);
    }

    /**
     * Wrap a result into a proxy and provide a class for cleaning up the resources in case no one
     * is waiting for the result. It is useful for closing Cursor object or something similar
     *
     * @param result method result
     * @param cleaner helper for cleaning up the resources if the result will not be delivered
     * @param <R> result type
     * @return Proxy which can be returned from the actor methods implementations
     */
    @NonNull
    public static <R> Promise<R> wrap(@Nullable R result, @Nullable ResourceCleaner<R> cleaner) {
        return new PromiseImpl<>(result, cleaner);
    }

    @NonNull
    public static <I, T extends MessageWithResult<I>, R> Promise<R> wrap(@NonNull MessageSender sender, @NonNull T message) {
        return new PromiseProxy<>(sender, message);
    }

    public interface Dispatcher<R> {
        void dispatch(@NonNull Promise<R> promise);
    }
}
