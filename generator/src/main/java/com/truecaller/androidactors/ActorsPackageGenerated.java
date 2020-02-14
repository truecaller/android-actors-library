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

import androidx.annotation.NonNull;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Modifier;
import java.util.List;

/* package */ class ActorsPackageGenerated implements ActorsPackageGenerator {

    /* package */ static final String FIELD_ERROR_HANDLER = "mFailureHandler";
    /* package */ static final String FIELD_PROXY_FACTORY = "mProxyFactory";
    /* package */ static final String METHOD_SET_ERROR_HANDLER = "setFailureHandler";
    /* package */ static final String METHOD_SET_PROXY_FACTORY = "setProxyFactory";
    /* package */ static final String METHOD_BUILD = "build";
    /* package */ static final String CLASS_PROXY_FACTORY = "ProxyFactoryImpl";

    @NotNull
    private final String mPackageName;

    @NotNull
    private final String mClassName;

    @NotNull
    private final ActorsPackage.AccessLevel mAccessLevel;

    /* package */ ActorsPackageGenerated(@NotNull ActorsPackageDescription description) {
        mPackageName = description.getPackageName();
        mClassName = description.getBuilderClassName();
        mAccessLevel = description.getAccessLevel();
    }

    @NotNull
    @Override
    public JavaFile generate(@NonNull List<? extends ActorInterfaceGenerated> interfaces) {
        final ClassName builderClass = ClassName.get(mPackageName, mClassName);
        final TypeSpec.Builder builder = TypeSpec.classBuilder(mClassName);

        builder.addModifiers(Modifier.FINAL);
        if (mAccessLevel == ActorsPackage.AccessLevel.Public) {
            builder.addModifiers(Modifier.PUBLIC);
        }

        builder.addField(FailureHandler.class, FIELD_ERROR_HANDLER, Modifier.PRIVATE);
        builder.addField(ProxyFactoryBase.class, FIELD_PROXY_FACTORY, Modifier.PRIVATE);

        builder.addMethod(generateHandlerSetter(builderClass));
        builder.addMethod(generateProxyFactorySetter(builderClass));

        TypeSpec threads = generateThreadsClass();
        TypeSpec proxyFactory = generateProxyFactory(interfaces);

        builder.addMethod(generateBuild(threads, proxyFactory, ClassName.get(CrashEarlyFailureHandler.class)));

        builder.addType(threads);
        builder.addType(proxyFactory);

        return JavaFile.builder(mPackageName, builder.build()).build();
    }

    @NotNull
    private MethodSpec generateHandlerSetter(@NotNull ClassName builderClass) {
        ParameterSpec param = ParameterSpec.builder(FailureHandler.class, "handler")
                .addAnnotation(NonNull.class)
                .build();

        MethodSpec.Builder method = MethodSpec.methodBuilder(METHOD_SET_ERROR_HANDLER)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(param)
                .addAnnotation(NonNull.class)
                .returns(builderClass);
        method.addStatement("this.$N = $N", FIELD_ERROR_HANDLER, "handler");
        method.addStatement("return this");
        return method.build();
    }

    @NotNull
    private MethodSpec generateProxyFactorySetter(@NotNull ClassName builderClass) {
        ParameterSpec param = ParameterSpec.builder(ProxyFactoryBase.class, "factory")
                .addAnnotation(NonNull.class)
                .build();

        MethodSpec.Builder method = MethodSpec.methodBuilder(METHOD_SET_PROXY_FACTORY)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(param)
                .addAnnotation(NonNull.class)
                .returns(builderClass);
        method.addStatement("this.$N = $N", FIELD_PROXY_FACTORY, "factory");
        method.addStatement("return this");
        return method.build();
    }

    @NotNull
    private MethodSpec generateBuild(@NonNull TypeSpec threads, @NonNull TypeSpec factory, @NonNull TypeName handler) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(METHOD_BUILD)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(NonNull.class)
                .returns(ActorsThreads.class);

        CodeBlock setHandler = CodeBlock.builder()
                .beginControlFlow(" if ($N == null)", FIELD_ERROR_HANDLER)
                .addStatement("$N = new $T()", FIELD_ERROR_HANDLER, handler)
                .endControlFlow()
                .build();

        CodeBlock setProxy = CodeBlock.builder()
                .beginControlFlow(" if ($N == null)", FIELD_PROXY_FACTORY)
                .addStatement("$N = new $N()", FIELD_PROXY_FACTORY, factory.name)
                .endControlFlow()
                .build();

        method.addCode(setHandler)
              .addCode(setProxy)
              .addStatement("return new $N($N, $N)", threads.name, FIELD_PROXY_FACTORY, FIELD_ERROR_HANDLER);

        return method.build();
    }

    @NotNull
    private TypeSpec generateThreadsClass() {
        TypeSpec.Builder threads = TypeSpec.classBuilder("ActorThreadsImpl")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .superclass(ActorsThreadsBase.class);

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addParameter(ProxyFactoryBase.class, "factory")
                .addParameter(FailureHandler.class, "handler")
                .addStatement("super($N, $N)", "factory", "handler")
                .build();

        threads.addMethod(constructor);
        return threads.build();
    }

    @NotNull
    private TypeSpec generateProxyFactory(@NonNull List<? extends ActorInterfaceGenerated> interfaces) {
        TypeSpec.Builder factory = TypeSpec.classBuilder(CLASS_PROXY_FACTORY)
                .addModifiers(Modifier.STATIC, Modifier.PRIVATE)
                .superclass(ProxyFactoryBase.class);

        TypeVariableName typeName = TypeVariableName.get("T");
        MethodSpec.Builder newProxy = MethodSpec.methodBuilder("newProxy")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addAnnotation(NonNull.class)
                .addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "$S", "unchecked").build())
                .returns(typeName)
                .addTypeVariable(typeName)
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), typeName), "cls")
                .addParameter(MessageSender.class, "sender");

        for (ActorInterfaceGenerated _interface : interfaces) {
            final ClassName proxy = ClassName.get(_interface.getProxyPackage(), _interface.getProxyName());
            final CodeBlock block = CodeBlock.builder()
                        .beginControlFlow("if ($T.equals($N))", proxy, "cls")
                        .addStatement("return (T) new $T($N)", proxy, "sender")
                        .endControlFlow()
                        .build();
            newProxy.addCode(block);
        }

        newProxy.addStatement("return defaultProxy($N, $N)", "cls", "sender");

        factory.addMethod(newProxy.build());

        return factory.build();
    }
}
