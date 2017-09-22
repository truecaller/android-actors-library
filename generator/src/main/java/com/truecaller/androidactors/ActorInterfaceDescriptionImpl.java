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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

/* package */ class ActorInterfaceDescriptionImpl implements ActorInterfaceDescription {

    @NotNull
    private final List<GenerationError> mErrors = new ArrayList<>();

    @Nullable
    private final TypeElement mElement;

    @NotNull
    private final List<Method> mMethods = new ArrayList<>();

    /* package */ ActorInterfaceDescriptionImpl(@NotNull Element element) {
        if (element.getKind() != ElementKind.INTERFACE) {
            mErrors.add(new GenerationError(GenerationError.ER0001, element));
            mElement = null;
            return;
        }

        mElement = (TypeElement) element;

        if (isPrivate(mElement)) {
            mErrors.add(new GenerationError(GenerationError.ER0002, mElement));
        }

        for (Element method : mElement.getEnclosedElements()) {
            if (method.getKind() != ElementKind.METHOD) {
                continue;
            }

            mMethods.add(createMethodRecord((ExecutableElement) method));
        }
    }

    @Override
    public boolean validate() {
        if (mElement == null) {
            assert !mErrors.isEmpty();
            return false;
        }

        for (Method method : mMethods) {
            method.validate(mErrors);
        }

        // First check that current state is clear
        for (GenerationError error : mErrors) {
            if (error.isError) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeProblems(@NotNull List<GenerationError> errors) {
        errors.addAll(mErrors);
    }

    private boolean isPrivate(@NotNull TypeElement element) {
        for (Modifier modifier : element.getModifiers()) {
            if (modifier == Modifier.PRIVATE) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public Iterable<? extends ActorInterfaceDescription.Method> methods() {
        return mMethods;
    }

    @NotNull
    @Override
    public TypeElement getType() {
        assert mElement != null;
        return mElement;
    }

    @NotNull
    private Method createMethodRecord(@NotNull ExecutableElement method) {
        List<? extends TypeMirror> exceptions = method.getThrownTypes();

        List<? extends TypeParameterElement> typeParameters = method.getTypeParameters();

        // TODO: Support var args methods
        List<Method.Argument> arguments = new ArrayList<>();
        for (VariableElement param : method.getParameters()) {
            Method.Argument argument = new Method.Argument(param);
            arguments.add(argument);
        }

        return new Method(method,
                method.getAnnotation(NonNull.class) != null,
                typeParameters != null && !typeParameters.isEmpty(),
                method.getReturnType(),
                exceptions != null && !exceptions.isEmpty(),
                arguments);
    }

    private static class Method implements ActorInterfaceDescription.Method {
        @NotNull
        private final ExecutableElement mElement;

        private final boolean mIsNonNull;

        private final boolean mHasTypeParameters;

        @NotNull
        private final TypeMirror mReturnType;

        @NotNull
        private final List<Argument> mArguments;

        private final boolean mHasExceptions;

        private Method(@NotNull ExecutableElement element,
                       boolean isNonNull,
                       boolean hasTypeParameters,
                       @NotNull TypeMirror returnType,
                       boolean hasExceptions,
                       @NotNull List<Argument> arguments) {
            mElement = element;
            this.mIsNonNull = isNonNull;
            this.mReturnType = returnType;
            this.mHasExceptions = hasExceptions;
            this.mHasTypeParameters = hasTypeParameters;
            this.mArguments = arguments;
        }

        void validate(List<GenerationError> errors) {
            if (mHasExceptions) {
                errors.add(new GenerationError(GenerationError.ER0003, mElement));
            }

            if (mHasTypeParameters) {
                errors.add(new GenerationError(GenerationError.ER0004, mElement));
            }

            if (mReturnType.getKind() != TypeKind.VOID) {
                boolean isInvalidType = true;

                if (mReturnType.getKind() == TypeKind.DECLARED) {
                    DeclaredType type = (DeclaredType) mReturnType;
                    if (Promise.class.getSimpleName().equals(type.asElement().getSimpleName().toString())) {
                        List<? extends TypeMirror> typeParams = type.getTypeArguments();
                        if (typeParams.isEmpty()) {
                            errors.add(new GenerationError(GenerationError.ER0007, mElement));
                        } else {
                            isInvalidType = false;
                        }
                    }
                }

                if (isInvalidType) {
                    errors.add(new GenerationError(GenerationError.ER0005, mElement));
                } else {
                    if (!mIsNonNull) {
                        errors.add(new GenerationError(GenerationError.ER0006, mElement));
                    }
                }
            }

            for (Argument argument : mArguments) {
                argument.validate(errors);
            }
        }

        @NotNull
        @Override
        public String getName() {
            return mElement.getSimpleName().toString();
        }

        @NotNull
        @Override
        public TypeMirror getType() {
            return mReturnType;
        }

        @Nullable
        @Override
        public TypeMirror getPromisedType() {
            if (mReturnType.getKind() == TypeKind.VOID) {
                return null;
            }

            final DeclaredType type = (DeclaredType) mReturnType;
            List<? extends TypeMirror> typeParams = type.getTypeArguments();
            return typeParams.get(0);
        }

        @Override
        public Iterable<? extends ActorInterfaceDescription.Method.Argument> arguments() {
            return mArguments;
        }

        private static class Argument implements ActorInterfaceDescription.Method.Argument {
            private final VariableElement mElement;

            @NotNull
            private final List<AnnotationMirror> mAnnotations;

            @NotNull
            private final String mName;

            @NotNull
            private final TypeMirror mType;

            private final int mSecureLevel;

            /* package */ Argument(@NotNull VariableElement element) {
                mElement = element;
                mName = element.getSimpleName().toString();
                mType = element.asType();

                SecureParameter secureLevel = element.getAnnotation(SecureParameter.class);
                if (secureLevel == null) {
                    mSecureLevel = SecureParameter.LEVEL_FULL_INFO;
                } else {
                    mSecureLevel = secureLevel.value();
                }

                mAnnotations = new ArrayList<>();
                for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
                    TypeElement annotationType = (TypeElement) annotation.getAnnotationType().asElement();
                    if (SecureParameter.class.getCanonicalName().equals(annotationType.getQualifiedName().toString())) {
                        continue;
                    }
                    mAnnotations.add(annotation);
                }
            }

            @NotNull
            @Override
            public String getName() {
                return mName;
            }

            @NotNull
            @Override
            public TypeMirror getType() {
                return mType;
            }

            @NotNull
            @Override
            public List<? extends AnnotationMirror> getAnnotations() {
                return mAnnotations;
            }

            @Override
            public int getSecureLevel() {
                return mSecureLevel;
            }

            void validate(List<GenerationError> errors) {
                if (mSecureLevel < SecureParameter.LEVEL_NO_INFO || mSecureLevel > SecureParameter.LEVEL_FULL_INFO) {
                    errors.add(new GenerationError(GenerationError.ER0011, mElement));
                }
            }
        }
    }
}
