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
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public interface ActorInterfaceDescription {

    @NotNull
    TypeElement getType();

    @NotNull
    Iterable<? extends ActorInterfaceDescription.Method> methods();

    boolean validate();

    void describeProblems(@NotNull List<GenerationError> errors);

    interface Method {
        @NotNull
        String getName();

        @NotNull
        TypeMirror getType();

        @Nullable
        TypeMirror getPromisedType();

        Iterable<? extends Argument> arguments();

        interface Argument {
            @NotNull
            String getName();

            @NotNull
            TypeMirror getType();

            @NotNull
            List<? extends AnnotationMirror> getAnnotations();
        }
    }
}
