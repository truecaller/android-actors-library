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
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@SupportedAnnotationTypes(value = {"com.truecaller.androidactors.ActorInterface", "com.truecaller.androidactors.ActorsPackage"})
public class ActorsProcessor extends AbstractProcessor {

    private Set<ProxyItem> mProxy;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mProxy = new HashSet<>();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            final JCodeModel model = new JCodeModel();
            final Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ActorInterface.class);
            final TypeUtils typeUtils = new TypeUtils(model);
            final ProxyMessageGenerator messageGenerator = new ProxyMessageGenerator(model, typeUtils);
            final ActorBuilderGenerator actorBuilderGenerator = new ActorBuilderGenerator(model, typeUtils);

            if (elements == null || elements.isEmpty()) {
                return true;
            }

            boolean hasNewProxy = false;
            for (Element element : elements) {
                if (element.getKind() != ElementKind.INTERFACE) {
                    processingEnv.getMessager().printMessage(Kind.ERROR,
                            "Only interfaces can be annotated by this annotation.",
                            element);
                    return false;
                }

                TypeElement typeElement = (TypeElement) element;

                if (!typeUtils.isPublic(typeElement)) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Actor interface must be public", typeElement);
                    return false;
                }

                JDefinedClass cls = generateProxy(model, typeUtils, messageGenerator, typeElement);
                if (cls == null) {
                    return false;
                }
                hasNewProxy = hasNewProxy | mProxy.add(new ProxyItem(typeElement, cls));
            }

            if (hasNewProxy && !buildActorsBuilder(roundEnv, actorBuilderGenerator ,mProxy)) {
                return false;
            }

            ActorsCodeWriter codeWriter = new ActorsCodeWriter(processingEnv.getFiler());
            ResourcesCodeWriter resourceWriter = new ResourcesCodeWriter(processingEnv.getFiler());
            try {
                model.build(codeWriter, resourceWriter);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
                return false;
            }

            return true;
        } catch (GeneratorException e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage(), e.getElement());
            return false;
        }
    }

    @Nullable
    private JDefinedClass generateProxy(@NotNull JCodeModel model,
                                        @NotNull TypeUtils typeUtils,
                                        @NotNull ProxyMessageGenerator messageGenerator,
                                        @NotNull TypeElement type) throws GeneratorException {
        final String className = buildProxyName(type);

        JDefinedClass cls;
        try {
            cls = model._class(JMod.PUBLIC | JMod.FINAL, className + "Proxy", ClassType.CLASS);
        } catch (JClassAlreadyExistsException e) {
            cls = e.getExistingClass();
        }
        JClass actorClass = model.ref(type.getQualifiedName().toString());
        cls = cls._implements(actorClass);

        JFieldVar sender = cls.field(JMod.FINAL | JMod.PRIVATE, MessageSender.class, "mMessageSender");
        JMethod constructor = cls.constructor(JMod.PUBLIC);
        JVar senderParam = constructor.param(MessageSender.class, "messageSender");
        constructor.body().assign(sender, senderParam);

        for (Element element : type.getEnclosedElements()) {
            if (element.getKind() != ElementKind.METHOD) {
                continue;
            }

            ExecutableElement method = (ExecutableElement) element;
            // check that method will not throw exception
            if (!method.getThrownTypes().isEmpty()) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Actor's methods can not throw exceptions", method);
                return null;
            }
            // check that return type is Promise or nothing
            boolean hasResult = false;
            TypeMirror methodResultType = method.getReturnType();
            JType returnType = model.VOID;
            if (methodResultType.getKind() != TypeKind.VOID) {
                hasResult = true;
                returnType = typeUtils.toJType(methodResultType);
                if (StringUtils.compare(returnType.erasure().binaryName(), Promise.class.getCanonicalName()) != 0) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Actor's methods can return only Promise", method);
                    return null;
                }

                if (method.getAnnotation(NonNull.class) == null) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Actor's methods which return Promise MUST be annotated by @NonNull annotation", method);
                    return null;
                }
            }

            final JClass message = messageGenerator.generateMessage(className, actorClass, method, hasResult);
            final JMethod mth = cls.method(JMod.PUBLIC | JMod.FINAL, returnType, method.getSimpleName().toString());
            mth.annotate(Override.class);
            if (returnType != model.VOID) {
                mth.annotate(NonNull.class);
            }
            final JInvocation newMessage = JExpr._new(message);
            final JInvocation invocation;
            if (hasResult) {
                invocation = model.ref(Promise.class).staticInvoke("wrap");
                invocation.arg(sender).arg(newMessage);
                mth.body()._return(invocation);
            } else {
                invocation = sender.invoke("deliver");
                invocation.arg(newMessage);
                mth.body().add(invocation);
            }

            // Pass RuntimeException as first parameter. It will be thrown in case of any exceptions
            // on actor's thread during processing message
            newMessage.arg(JExpr._new(model._ref(ActorMethodInvokeException.class)));

            for (VariableElement var : method.getParameters()) {
                JVar param = mth.param(typeUtils.toJType(var.asType()), var.getSimpleName().toString());
                if (typeUtils.isNotNull(var)) {
                    param.annotate(NonNull.class);
                }
                newMessage.arg(param);
            }
        }

        return cls;
    }

    @NotNull
    private String buildProxyName(@NotNull TypeElement type) {
        StringBuilder result = new StringBuilder();

        for(;;) {
            result.insert(0, type.getSimpleName());
            Element element = type.getEnclosingElement();
            if (element.getKind() == ElementKind.PACKAGE) {
                result.insert(0, '.');
                result.insert(0, ((PackageElement) element).getQualifiedName());
                return result.toString();
            } else {
                result.insert(0, '_');
                type = (TypeElement) element;
            }
        }
    }

    private boolean buildActorsBuilder(@NotNull RoundEnvironment roundEnvironment,
                                       @NotNull ActorBuilderGenerator generator,
                                       @NotNull Set<ProxyItem> proxyItems) {
        Set<? extends Element> packages = roundEnvironment.getElementsAnnotatedWith(ActorsPackage.class);

        PackageElement pkg = null;
        if (packages != null) {
            for (Element element : packages) {
                if (element.getKind() != ElementKind.PACKAGE) {
                    continue;
                }

                if (pkg != null) {
                    processingEnv.getMessager().printMessage(Kind.ERROR, "Only one package can be marked by @ActorsPackage", pkg);
                    return false;
                }

                pkg = (PackageElement) element;
            }
        }

        if (pkg == null) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "You MUST mark package which will contain actors builder by @ActorsPackage annotation");
            return false;
        }

        return generator.generate(pkg, pkg.getAnnotation(ActorsPackage.class), proxyItems);
    }
}
