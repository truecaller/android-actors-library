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
import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;

/* package */ class ProxyMessageGenerator {

    @NotNull
    private final JCodeModel mModel;

    @NotNull
    private final TypeUtils mTypeUtils;

    /* package */ ProxyMessageGenerator(@NotNull JCodeModel model,
                                        @NotNull TypeUtils typeUtils) {
        mModel = model;
        mTypeUtils = typeUtils;
    }

    /* package */ JClass generateMessage(@NotNull String proxyQualifiedName,
                                         @NotNull JClass actorClass, @NotNull ExecutableElement method,
                                         @Nullable JClass result) throws GeneratorException {
        JDefinedClass cls;
        int index = 0;
        for (;;++index) {
            try {
                StringBuilder messageClassName = new StringBuilder();
                messageClassName.append(proxyQualifiedName);
                messageClassName.append(StringUtils.capitalize(method.getSimpleName().toString()));
                messageClassName.append("Message");
                if (index > 0) {
                    messageClassName.append(index);
                }

                cls = mModel._class(JMod.PUBLIC | JMod.FINAL, messageClassName.toString(), ClassType.CLASS);
                break;
            } catch (JClassAlreadyExistsException e) {
                // nothing
            }
        }

        generateConstructors(cls, method.getParameters());
        generateToString(cls, method.getSimpleName().toString(), method.getParameters());

        JClass superClass = mModel.ref(MessageBase.class).narrow(actorClass);
        if (result == null) {
            superClass = superClass.narrow(Void.class);
        } else {
            superClass = superClass.narrow(result);
        }
        cls._extends(superClass);

        generateInvoke(cls, actorClass, method.getSimpleName().toString(), method.getParameters(), result);

        return cls;
    }

    private void generateConstructors(@NotNull JDefinedClass cls,
                                      @NotNull List<? extends VariableElement> parameters) throws GeneratorException {
        final JMethod constructor = cls.constructor(JMod.NONE);
        final JVar exceptionParam = constructor.param(mModel._ref(ActorMethodInvokeException.class), "exception");
        constructor.body().invoke("super").arg(exceptionParam);

        for (VariableElement var : parameters) {
            final String name = var.getSimpleName().toString();
            final JType paramType = mTypeUtils.toJType(var.asType());
            cls.field(JMod.FINAL | JMod.PRIVATE, paramType, name);
            // direct constructor
            JVar param = constructor.param(paramType, name);
            constructor.body().assign(JExpr.refthis(name), param);
        }
    }

    private void generateInvoke(@NotNull JDefinedClass cls,
                                    @NotNull JClass actorClass,
                                    @NotNull String methodName,
                                    @NotNull List<? extends VariableElement> parameters,
                                    @Nullable JClass resultType) {
        JClass result = mModel.ref(Promise.class);
        if (resultType == null) {
            result = result.narrow(Void.class);
        } else {
            result = result.narrow(resultType);
        }
        final JMethod invokeMethod = cls.method(JMod.PUBLIC, result, "invoke");
        JVar target = invokeMethod.param(actorClass, "target");
        invokeMethod.annotate(Override.class);
        invokeMethod.annotate(Nullable.class);
        target.annotate(NonNull.class);
        JInvocation call = target.invoke(methodName);
        for (VariableElement var : parameters) {
            call.arg(cls.fields().get(var.getSimpleName().toString()));
        }

        if (resultType != null) {
            call = JExpr.invoke("verifyResult").arg(call);
            invokeMethod.body()._return(call);
        } else {
            invokeMethod.body().add(call);
            invokeMethod.body()._return(JExpr._null());
        }
    }

    private void generateToString(@NotNull JDefinedClass cls, @NotNull String methodName,
                                  @NotNull List<? extends VariableElement> parameters) {
        final JMethod invokeMethod = cls.method(JMod.PUBLIC, String.class, "toString");
        invokeMethod.annotate(Override.class);

        JExpression result = invokeMethod.body().decl(mModel.ref(String.class), "result")
                .init(JExpr.lit("." + methodName + "("));

        if (!parameters.isEmpty()) {
            VariableElement field = parameters.get(0);
            result = JOp.plus(result, cls.fields().get(field.getSimpleName().toString()));
            for (int index = 1; index < parameters.size(); ++index) {
                field = parameters.get(index);
                result = JOp.plus(result, JExpr.lit(", "));
                result = JOp.plus(result, cls.fields().get(field.getSimpleName().toString()));
            }
        }
        result = JOp.plus(result, JExpr.lit(")"));

        invokeMethod.body()._return(result);
    }
}
