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

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

/* package */ class GenerationError {
    /* package */ static final String ER0001 = "Only interfaces can be annotated by @ActorInterface annotation";
    /* package */ static final String ER0002 = "Actor interface can't be private";
    /* package */ static final String ER0003 = "Actor's methods can not throw exceptions";
    /* package */ static final String ER0004 = "Actor's methods can not be generic methods";
    /* package */ static final String ER0005 = "Actor's methods can return only Promise";
    /* package */ static final String ER0006 = "Actor's methods which return Promise MUST be annotated by @NonNull annotation";
    /* package */ static final String ER0007 = "Specify promised type";
    /* package */ static final String ER0008 = "Only packages can be annotated by @ActorsPackage annotation";
    /* package */ static final String ER0009 = "Only one package can be marked by @ActorsPackage";
    /* package */ static final String ER0010 = "You MUST mark package which will contain actors builder by @ActorsPackage annotation";


    final boolean isError;

    @NotNull
    /* package */ final String message;

    @Nullable
    /* package */ final Element element;

    /* package */ GenerationError(boolean isError, @NotNull String message, @Nullable Element element) {
        this.isError = isError;
        this.message = message;
        this.element = element;
    }

    /* package */ GenerationError(@NotNull String message, @Nullable Element element) {
        this(true, message, element);
    }

    /* package */ GenerationError(@NotNull String message) {
        this(true, message, null);
    }

    /* package */ void print(@NotNull Messager messager) {
        Diagnostic.Kind kind = isError ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
        if (element != null) {
            messager.printMessage(kind, message, element);
        } else {
            messager.printMessage(kind, message);
        }
    }
}
