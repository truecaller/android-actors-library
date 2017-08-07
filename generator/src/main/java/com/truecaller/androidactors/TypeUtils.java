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

import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;
import org.jetbrains.annotations.NotNull;

import android.support.annotation.NonNull;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/* package */ class TypeUtils {

    @NotNull
    private final JCodeModel mModel;

    /* package */ TypeUtils(@NotNull JCodeModel model) {
        mModel = model;
    }

    /* package */ JType toJType(@NotNull TypeMirror mirror) {

        TypeKind kind = mirror.getKind();

        if (kind == TypeKind.VOID) {
            return mModel.VOID;
        }

        if (kind.isPrimitive()) {
            switch (kind) {
                case BOOLEAN:
                    return mModel.BOOLEAN;
                case BYTE:
                    return mModel.BYTE;
                case SHORT:
                    return mModel.SHORT;
                case INT:
                    return mModel.INT;
                case LONG:
                    return mModel.LONG;
                case CHAR:
                    return mModel.CHAR;
                case FLOAT:
                    return mModel.FLOAT;
                case DOUBLE:
                    return mModel.DOUBLE;
            }
        }

        if (kind == TypeKind.ARRAY && mirror instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) mirror;
            JType result = toJType(arrayType.getComponentType());
            return result.array();
        }

        if (kind == TypeKind.DECLARED && mirror instanceof DeclaredType) {
            final DeclaredType declaredType = (DeclaredType) mirror;
            final TypeElement typeElement = (TypeElement) declaredType.asElement();

            final String className = typeElement.getQualifiedName().toString();
            JClass result;
            try {
                result = mModel.ref(className);
            } catch (NoClassDefFoundError e) {
                // JCodeModel does not handle NoClassDefFoundError exception, but we should
                // If we are not able to get class reference - do the same as JCodeModel does -
                // create instance of JDirectClass
                result = createDefinedClass(className);
            }

            // if it is a generic add them to type
            List<? extends TypeMirror> typeParams = declaredType.getTypeArguments();
            if (typeParams != null) {
                for (TypeMirror type : typeParams) {
                    JType arg = toJType(type);
                    result = result.narrow(arg);
                }
            }

            return result;
        }

        return mModel.ref(mirror.toString());
    }

    /* package */ boolean isNotNull(@NotNull VariableElement variable) {
        return variable.getAnnotation(NonNull.class) != null;
    }

    /* package */ boolean isPublic(@NotNull TypeElement element) {
        for (Modifier modifier : element.getModifiers()) {
            if (modifier == Modifier.PUBLIC) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private JClass createDefinedClass(String fullQualifiedName) {
        try {
            //noinspection unchecked
            Class<?> cls = getClass().getClassLoader().loadClass("com.sun.codemodel.JDirectClass");
            Constructor<?> constructor = cls.getConstructor(JCodeModel.class, String.class);
            constructor.setAccessible(true);
            return (JClass) constructor.newInstance(mModel, fullQualifiedName);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
