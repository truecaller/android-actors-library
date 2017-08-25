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

import com.google.common.base.Predicate;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* package */ class Predicates {
    /* package */ static class Type implements Predicate<TypeSpec> {
        @Nullable
        private final String mName;

        /* package */ Type(@Nullable String name) {
            mName = name;
        }

        @Override
        public boolean apply(TypeSpec typeSpec) {
            return typeSpec.name.equals(mName);
        }
    }

    /* package */ static class Method implements Predicate<MethodSpec> {
        @Nullable
        private final String mName;

        /* package */ Method(@Nullable String name) {
            mName = name;
        }

        @Override
        public boolean apply(MethodSpec methodSpec) {
            return methodSpec.name.equals(mName);
        }
    }

    /* package */ static class Field implements Predicate<FieldSpec> {
        @Nullable
        private final String mName;

        /* package */ Field(@Nullable String name) {
            mName = name;
        }

        @Override
        public boolean apply(FieldSpec fieldSpec) {
            return fieldSpec.name.equals(mName);
        }
    }

    /* package */ static class Annotation implements Predicate<AnnotationSpec> {
        @NotNull
        private final TypeName mAnnotation;

        /* package */ Annotation(@NotNull Class<? extends java.lang.annotation.Annotation> cls) {
            mAnnotation = ClassName.get(cls);
        }

        @Override
        public boolean apply(AnnotationSpec annotationSpec) {
            return mAnnotation.equals(annotationSpec.type);
        }
    }
}
