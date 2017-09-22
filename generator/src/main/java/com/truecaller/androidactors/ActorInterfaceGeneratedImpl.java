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
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* package */ class ActorInterfaceGeneratedImpl implements ActorInterfaceGenerator {

    private static final String MESSAGE_SENDER_FIELD = "mMessageSender";
    private static final String MESSAGE_SENDER_PARAM = "messageSender";

    @NotNull
    private final TypeElement mActor;

    @NotNull
    private final List<Message> mMessages = new ArrayList<>();

    /* package */ ActorInterfaceGeneratedImpl(@NotNull ActorInterfaceDescription _interface) {
        mActor = _interface.getType();
        for (ActorInterfaceDescription.Method method : _interface.methods()) {
            mMessages.add(new Message(method));
        }
    }

    @NotNull
    public JavaFile generate(NamesProvider namesProvider) {
        TypeSpec.Builder _class = TypeSpec.classBuilder(getProxyName());
        _class.addModifiers(Modifier.FINAL, Modifier.PUBLIC);
        _class.addField(MessageSender.class, MESSAGE_SENDER_FIELD, Modifier.PRIVATE, Modifier.FINAL);
        _class.addSuperinterface(ClassName.get(mActor));
        _class.addJavadoc("@hide");

        _class.addMethod(generateConstructor());
        _class.addMethod(generateCompareMethod());

        TypeName actorType = TypeName.get(mActor.asType());
        for (Message message : mMessages) {
            TypeSpec messageType = message.generate(actorType, namesProvider);
            _class.addType(messageType);
            _class.addMethod(generateMethod(message, messageType));

        }

        return JavaFile.builder(extractActorPackage(mActor), _class.build()).build();
    }

    @NotNull
    @Override
    public String getProxyName() {
        return extractActorName(mActor) + "$Proxy";
    }

    @NotNull
    @Override
    public String getProxyPackage() {
        return extractActorPackage(mActor);
    }

    @NotNull
    private String extractActorName(@NotNull Element element) {
        StringBuilder result = new StringBuilder();
        while (element != null) {
            if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE) {
                if (result.length() > 0) {
                    result.insert(0, '$');
                }
                result.insert(0, element.getSimpleName());
            }
            element = element.getEnclosingElement();
        }
        return result.toString();
    }

    @NotNull
    private String extractActorPackage(@NotNull Element element) {
        while (element != null) {
            if (element.getKind() == ElementKind.PACKAGE) {
                return ((PackageElement) element).getQualifiedName().toString();
            }
            element = element.getEnclosingElement();
        }
        return "";
    }

    @NotNull
    private MethodSpec generateConstructor() {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder();
        constructor.addModifiers(Modifier.PUBLIC);
        constructor.addParameter(MessageSender.class, MESSAGE_SENDER_PARAM);
        constructor.addStatement("$N = $N", MESSAGE_SENDER_FIELD, MESSAGE_SENDER_PARAM);
        return constructor.build();
    }

    @NotNull
    private MethodSpec generateCompareMethod() {
        MethodSpec.Builder method = MethodSpec.methodBuilder("equals")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .returns(TypeName.BOOLEAN)
                .addParameter(Class.class, "cls");
        method.addStatement("return $T.class.equals($N)", TypeName.get(mActor.asType()), "cls");
        return method.build();
    }

    @NotNull
    private MethodSpec generateMethod(@NotNull Message message, @NotNull TypeSpec messageClass) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(message.methodName);
        method.addModifiers(Modifier.PUBLIC);
        method.addAnnotation(Override.class);

        final TypeName promise;
        StringBuilder call = new StringBuilder();
        if (message.resultType == null) {
            promise = TypeName.VOID;
            call.append(MESSAGE_SENDER_FIELD).append(".deliver(");
        } else {
            method.addAnnotation(NonNull.class);
            promise = ParameterizedTypeName.get(ClassName.get(Promise.class), message.resultType);
            call.append("return Promise.wrap(").append(MESSAGE_SENDER_FIELD).append(", ");
        }
        method.returns(promise);
        call.append("new ").append(messageClass.name).append("(new ActorMethodInvokeException()");

        for (Message.Argument argument : message.arguments) {
            method.addParameter(argument.generate());
            call.append(", ").append(argument.name);
        }
        call.append("))");

        method.addStatement(call.toString());

        return method.build();
    }

    /* package */ static class Message {

        @NotNull
        final String methodName;

        @Nullable
        final TypeName resultType;

        final List<Argument> arguments = new ArrayList<>();

        /* package */ Message(@NotNull ActorInterfaceDescription.Method method) {
            methodName = method.getName();
            TypeMirror promised = method.getPromisedType();
            resultType = promised != null ? TypeName.get(promised) : null;


            for (ActorInterfaceDescription.Method.Argument argument : method.arguments()) {
                arguments.add(new Argument(argument));
            }
        }

        /* package */ TypeSpec generate(@NotNull TypeName actor, @NotNull NamesProvider namesProvider) {
            TypeName result = resultType == null ? TypeName.get(Void.class) : resultType;
            TypeSpec.Builder _class = TypeSpec.classBuilder(namesProvider.buildMessageName(methodName));
            ParameterizedTypeName baseClass = ParameterizedTypeName.get(ClassName.get(MessageBase.class), actor, result);
            _class.superclass(baseClass);
            _class.addModifiers(Modifier.PRIVATE, Modifier.STATIC);

            _class.addMethod(createConstructor(_class));
            _class.addMethod(createInvoke(actor, result));
            _class.addMethod(createToString());

            return _class.build();
        }

        @NotNull
        private MethodSpec createConstructor(@NotNull TypeSpec.Builder _class) {
            MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE);
            constructor.addParameter(ClassName.get(ActorMethodInvokeException.class), "exception");
            constructor.addStatement("super(exception)");
            for (Argument argument : arguments) {
                _class.addField(argument.type, argument.name, Modifier.PRIVATE, Modifier.FINAL);
                constructor.addParameter(argument.type, argument.name);
                constructor.addStatement("this.$N = $N", argument.name, argument.name);
            }
            return constructor.build();
        }

        @NotNull
        private MethodSpec createInvoke(@NotNull TypeName actor, @NotNull TypeName result) {
            ClassName promise = ClassName.get(Promise.class);
            MethodSpec.Builder method = MethodSpec.methodBuilder("invoke")
                    .returns(ParameterizedTypeName.get(promise, result))
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(actor, "target");

            final StringBuilder methodCall = new StringBuilder();
            if (resultType != null) {
                method.addAnnotation(NonNull.class);
                methodCall.append("return verifyResult(");
            }
            methodCall.append("target.").append(methodName).append('(');
            Iterator<Argument> args = arguments.iterator();
            if (args.hasNext()) {
                methodCall.append("this.").append(args.next().name);
                while (args.hasNext()) {
                    methodCall.append(", this.").append(args.next().name);
                }
            }
            methodCall.append(')');
            if (resultType != null) {
                methodCall.append(')');
            }
            method.addStatement(methodCall.toString());
            if (resultType == null) {
                method.addStatement("return null");
            }
            return method.build();
        }

        @NotNull
        private MethodSpec createToString() {
            MethodSpec.Builder method = MethodSpec.methodBuilder("toString")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(String.class)
                    .addAnnotation(Override.class);

            StringBuilder result = new StringBuilder("return \".");
            result.append(methodName).append("(\"");

            if (!arguments.isEmpty()) {
                Iterator<Argument> it = arguments.iterator();
                result.append(" + ");
                appendParameterValue(result, it.next());
                while (it.hasNext()) {
                    result.append(" + \",\" + ");
                    appendParameterValue(result, it.next());
                }
            }
            result.append(" + \")\"");

            method.addStatement(result.toString());

            return method.build();
        }

        private void appendParameterValue(@NonNull StringBuilder result, @NonNull Argument argument) {
            result.append("logParam(").append(argument.name).append(",");
            result.append(argument.secureLevel).append(")");
        }

        static class Argument {
            @NotNull
            /* package */ final String name;

            @NotNull
            /* package */ final TypeName type;

            /* package */ final List<? extends AnnotationMirror> annotations;

            /* package */ final int secureLevel;

            /* package */ Argument(@NotNull ActorInterfaceDescription.Method.Argument argument) {
                name = argument.getName();
                type = TypeName.get(argument.getType());
                annotations = argument.getAnnotations();
                secureLevel = argument.getSecureLevel();
            }

            ParameterSpec generate() {
                ParameterSpec.Builder builder = ParameterSpec.builder(type, name);
                for (AnnotationMirror annotation : annotations) {
                    AnnotationSpec spec = AnnotationSpec.get(annotation);
                    builder.addAnnotation(spec);
                }
                return builder.build();
            }
        }
    }
}
