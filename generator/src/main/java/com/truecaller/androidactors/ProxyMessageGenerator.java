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
import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JType;
import com.sun.codemodel.JTypeVar;
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
                                         boolean hasResult) throws GeneratorException {
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

        JFieldVar exception = cls.field(JMod.PRIVATE | JMod.FINAL, ActorMethodInvokeException.class, "mException");
        generateConstructors(cls, exception, method.getParameters());
        generateToString(cls, method.getSimpleName().toString(), method.getParameters());
        generateThrowableGetter(cls, exception);

        if (!hasResult) {
            cls._implements(mModel.ref(Message.class).narrow(actorClass));
            generateVoidInvoke(cls, actorClass, method.getSimpleName().toString(), method.getParameters());
        } else {
            cls._implements(mModel.ref(MessageWithResult.class).narrow(actorClass));
            generateResultInvoke(cls, actorClass, method.getSimpleName().toString(), method.getParameters());
        }

        return cls;
    }

    private void generateConstructors(@NotNull JDefinedClass cls,
                                      @NotNull JFieldVar exception,
                                      @NotNull List<? extends VariableElement> parameters) throws GeneratorException {
        final JMethod constructor = cls.constructor(JMod.NONE);
        final JVar exceptionParam = constructor.param(mModel._ref(ActorMethodInvokeException.class), "exception");
        constructor.body().assign(exception, exceptionParam);

        for (VariableElement var : parameters) {
            final String name = var.getSimpleName().toString();
            final JType paramType = mTypeUtils.toJType(var.asType());
            cls.field(JMod.FINAL | JMod.PRIVATE, paramType, name);
            // direct constructor
            JVar param = constructor.param(paramType, name);
            constructor.body().assign(JExpr.refthis(name), param);
        }
    }

    private void generateVoidInvoke(@NotNull JDefinedClass cls, @NotNull JClass actorClass, @NotNull String methodName,
                                    @NotNull List<? extends VariableElement> parameters) {
        final JMethod invokeMethod = cls.method(JMod.PUBLIC, void.class, "invoke");
        JVar target = invokeMethod.param(actorClass, "target");
        invokeMethod.annotate(Override.class);
        JInvocation call = target.invoke(methodName);
        invokeMethod.body().add(call);
        for (VariableElement var : parameters) {
            call.arg(cls.fields().get(var.getSimpleName().toString()));
        }
    }

    private void generateThrowableGetter(@NotNull JDefinedClass cls, @NotNull JFieldVar exception) {
        final JMethod exceptionMethod = cls.method(JMod.PUBLIC, ActorMethodInvokeException.class, "exception");
        exceptionMethod.annotate(Override.class);
        exceptionMethod.annotate(NonNull.class);
        exceptionMethod.body()._return(exception);
    }

    private void generateResultInvoke(@NotNull JDefinedClass cls, @NotNull JClass actorClass, @NotNull String methodName,
                                      @NotNull List<? extends VariableElement> parameters) {
        final JMethod invokeMethod = cls.method(JMod.PUBLIC, void.class, "invoke");
        invokeMethod.annotate(SuppressWarnings.class).param("value", "unchecked");
        JTypeVar T = invokeMethod.generify("T");

        JClass resultSender = mModel.ref(Promise.Dispatcher.class);
        resultSender = resultSender.narrow(T);

        JClass promiseRef = mModel.ref(Promise.class);

        JVar target = invokeMethod.param(actorClass, "target");
        JVar promise = invokeMethod.param(resultSender, "promise");

        invokeMethod.annotate(Override.class);
        JInvocation call = target.invoke(methodName);
        JVar result = invokeMethod.body().decl(promiseRef, "result").init(call);
        invokeMethod.body().add(promise.invoke("dispatch").arg(result));

        for (VariableElement var : parameters) {
            call.arg(cls.fields().get(var.getSimpleName().toString()));
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
