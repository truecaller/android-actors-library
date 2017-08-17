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
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JTypeVar;
import com.sun.codemodel.JVar;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.PackageElement;
import java.util.Set;

/* package */ class ActorBuilderGenerator {
    @NotNull
    private final JCodeModel mModel;

    /* package */ ActorBuilderGenerator(@NotNull JCodeModel model) {
        mModel = model;
    }

    /*package */ boolean generate(@NotNull PackageElement pckg,
                                  @NotNull ActorsPackage annotation,
                                  @NotNull Set<ProxyItem> items) {

        JDefinedClass builder;
        int modifiers = JMod.FINAL;

        switch (annotation.access()) {
            case Public:
                modifiers |= JMod.PUBLIC;
                break;
            case Package:
                break;
        }

        final String name = pckg.getQualifiedName() + "." + annotation.builderName();
        try {
            builder = mModel._class(modifiers, name, ClassType.CLASS);
        } catch (JClassAlreadyExistsException e) {
            return true;
        }

        JFieldVar failureHandler = builder.field(JMod.PRIVATE, FailureHandler.class, "mFailureHandler");
        JFieldVar proxy = builder.field(JMod.PRIVATE, ProxyFactoryBase.class, "mProxy");
        generateBuilderSetters(builder, failureHandler, proxy);

        // Build proxy factory
        JDefinedClass proxyFactory = generateProxyFactory(builder, items);
        assert proxyFactory != null;

        // Build actors implementation
        JDefinedClass impl = generateActorsThreadsImpl(builder);
        assert impl != null;

        generateBuilderBuildMethod(builder, impl, proxyFactory, failureHandler, proxy);

        return true;
    }

    private void generateBuilderSetters(@NotNull JDefinedClass builder,
                                        @NotNull JFieldVar failureHandler,
                                        @NotNull JFieldVar proxy) {
        JMethod setProxy = builder.method(JMod.PUBLIC, builder, "setProxy");
        JVar proxyArg = setProxy.param(ProxyFactoryBase.class, "proxy");
        proxyArg.annotate(NonNull.class);
        setProxy.body().assign(proxy, proxyArg);
        setProxy.body()._return(JExpr._this());

        JMethod setFailureHandler = builder.method(JMod.PUBLIC, builder, "setFailureHandler");
        JVar handlerArg = setFailureHandler.param(FailureHandler.class, "handler");
        handlerArg.annotate(NonNull.class);
        setFailureHandler.body().assign(failureHandler, handlerArg);
        setFailureHandler.body()._return(JExpr._this());
    }

    private void generateBuilderBuildMethod(@NotNull JDefinedClass builder,
                                            @NotNull JDefinedClass impl,
                                            @NonNull JDefinedClass proxyClass,
                                            @NotNull JFieldVar failureHandler,
                                            @NotNull JFieldVar proxy) {
        JMethod build = builder.method(JMod.PUBLIC, ActorsThreads.class, "build");
        JExpression proxyExpr = JOp.cond(JOp.eq(proxy, JExpr._null()), JExpr._new(proxyClass), proxy);
        JExpression handlerExpr = JOp.cond(JOp.eq(failureHandler, JExpr._null()), JExpr._new(mModel._ref(CrashEarlyFailureHandler.class)), failureHandler);
        build.body()._return(JExpr._new(impl).arg(proxyExpr).arg(handlerExpr));
    }

    @Nullable
    private JDefinedClass generateProxyFactory(@NotNull JDefinedClass rootClass,
                                               @NotNull Set<ProxyItem> items) {
        JDefinedClass cls;
        try {
            cls = rootClass._class(JMod.PRIVATE | JMod.FINAL,  "ProxyFactoryImpl", ClassType.CLASS);
        } catch (JClassAlreadyExistsException e) {
            return null;
        }

        cls._extends(ProxyFactoryBase.class);

        JMethod newProxy = cls.method(JMod.PUBLIC, void.class, "newProxy");
        JTypeVar type = newProxy.generify("T");
        newProxy.type(type);
        newProxy.annotate(Override.class);
        newProxy.annotate(NonNull.class);
        newProxy.annotate(SuppressWarnings.class).param("value", "unchecked");
        final JVar paramCls = newProxy.param(mModel.ref(Class.class).narrow(type), "cls");
        paramCls.annotate(NonNull.class);
        final JVar paramSender = newProxy.param(mModel.ref(MessageSender.class), "postman");
        paramSender.annotate(NonNull.class);

        for (ProxyItem item : items) {
            JClass proxyType = mModel.ref(item.type.getQualifiedName().toString());
            JConditional cond = newProxy.body()._if(paramCls.eq(proxyType.dotclass()));
            JInvocation newItem = JExpr._new(item.proxyClass);
            newItem.arg(paramSender);
            cond._then()._return(JExpr.cast(type, newItem));
        }

        JExpression superCall = JExpr._super().invoke("defaultProxy")
                .arg(paramCls).arg(paramSender);
        newProxy.body()._return(superCall);

        return cls;
    }

    @Nullable
    private JDefinedClass generateActorsThreadsImpl(@NotNull JDefinedClass rootClass) {

        JDefinedClass result;
        try {
            result = rootClass._class(JMod.PRIVATE | JMod.FINAL, "ActorThreadsImpl", ClassType.CLASS);
        } catch (JClassAlreadyExistsException e) {
            return null;
        }

        result._extends(ActorsThreadsBase.class);
        result._implements(ActorsThreads.class);

        generateImplConstructor(result);

        return result;
    }

    private void generateImplConstructor(@NotNull JDefinedClass impl) {
        JMethod constr = impl.constructor(JMod.NONE);
        JVar proxyArg = constr.param(ProxyFactoryBase.class, "proxy");
        proxyArg.annotate(NonNull.class);
        JVar handlerArg = constr.param(FailureHandler.class, "failureHandler");
        handlerArg.annotate(NonNull.class);
        constr.body().invoke("super").arg(proxyArg).arg(handlerArg);
    }

}
