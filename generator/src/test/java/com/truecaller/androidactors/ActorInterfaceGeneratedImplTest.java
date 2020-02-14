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
import com.google.common.collect.Iterables;
import com.google.testing.compile.CompilationRule;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.truecaller.androidactors.ActorInterfaceDescription.Method;
import com.truecaller.androidactors.cases.ActorContainerClass;
import com.truecaller.androidactors.cases.ActorContainerInterface;
import com.truecaller.androidactors.cases.SimpleActor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor6;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("SameParameterValue")
public class ActorInterfaceGeneratedImplTest {
    @Rule
    public final CompilationRule rule = new CompilationRule();

    @Mock
    private ActorInterfaceDescription mDescription;

    @Mock
    private NamesProvider mNamesProvider;

    private static AnnotationMirror sNonNullMirror;

    @Before
    public void setUp() {
        fetchNonNullAnnotation();

        MockitoAnnotations.initMocks(this);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0];
            }
        }).when(mNamesProvider).buildMessageName(Mockito.anyString());

        Mockito.doReturn(new ArrayList<Method>()).when(mDescription).methods();
    }

    private void fetchNonNullAnnotation() {
        TypeElement type = rule.getElements().getTypeElement(SimpleActor.class.getCanonicalName());
        for (Element element : type.getEnclosedElements()) {
            AnnotationMirror annotation = element.accept(new SimpleElementVisitor6<AnnotationMirror, Void>() {
                @Override
                public AnnotationMirror visitExecutable(ExecutableElement e, Void aVoid) {
                    for (AnnotationMirror a : e.getAnnotationMirrors()) {
                        TypeElement type = (TypeElement) a.getAnnotationType().asElement();
                        if (NonNull.class.getCanonicalName().equals(type.getQualifiedName().toString())) {
                            return a;
                        }
                    }
                    return super.visitExecutable(e, aVoid);
                }
            }, null);

            if (annotation != null) {
                sNonNullMirror = annotation;
                break;
            }
        }
    }

    private void setTypeElement(Class cls) {
        TypeElement type = rule.getElements().getTypeElement(cls.getCanonicalName());
        Mockito.doReturn(type).when(mDescription).getType();
    }

    private Method.Argument argument(@NotNull String name, @NotNull TypeKind kind) {
        TypeMirror type = rule.getTypes().getPrimitiveType(kind);
        return argument(name, type, false);
    }

    private Method.Argument argument(@NotNull String name, @NotNull String typeName, boolean nonNull) {
        TypeMirror type = rule.getElements().getTypeElement(typeName).asType();
        return argument(name, type, nonNull);
    }

    private Method.Argument argument(@NotNull String name, @NotNull TypeMirror type, boolean nonNull) {
        Method.Argument result = Mockito.mock(Method.Argument.class);
        Mockito.doReturn(name).when(result).getName();
        Mockito.doReturn(type).when(result).getType();
        if (nonNull) {
            List<AnnotationMirror> annotations = new ArrayList<>();
            annotations.add(sNonNullMirror);
            Mockito.doReturn(annotations).when(result).getAnnotations();
        }
        return result;
    }

    private ActorInterfaceDescription.Method method(@NotNull String name, @NotNull String resultTypeName, Method.Argument... args) {
        TypeElement resultType = rule.getElements().getTypeElement(resultTypeName);
        return method(name, resultType.asType(), args);
    }

    private ActorInterfaceDescription.Method method(@NotNull String name, Method.Argument... args) {
        return method(name, (TypeMirror) null, args);
    }

    private ActorInterfaceDescription.Method method(@NotNull String name, @Nullable TypeMirror resultType, Method.Argument... args) {
        ActorInterfaceDescription.Method result = Mockito.mock(ActorInterfaceDescription.Method.class);
        Mockito.doReturn(name).when(result).getName();
        Mockito.doReturn(resultType).when(result).getPromisedType();

        List<Method.Argument> arguments = new ArrayList<>();
        Collections.addAll(arguments, args);
        Mockito.doReturn(arguments).when(result).arguments();

        return result;
    }

    @Test
    public void generate_flatClassName_SimpleActor() {
        setTypeElement(SimpleActor.class);

        ActorInterfaceGeneratedImpl generated = new ActorInterfaceGeneratedImpl(mDescription);
        JavaFile file = generated.generate(mNamesProvider);

        Assert.assertEquals("com.truecaller.androidactors.cases", file.packageName);
        Assert.assertEquals("SimpleActor$Proxy", file.typeSpec.name);
    }

    @Test
    public void generate_nestedInInterfaceName_NestedActor() {
        setTypeElement(ActorContainerInterface.NestedActor.class);

        ActorInterfaceGeneratedImpl generated = new ActorInterfaceGeneratedImpl(mDescription);
        JavaFile file = generated.generate(mNamesProvider);

        Assert.assertEquals("com.truecaller.androidactors.cases", file.packageName);
        Assert.assertEquals("ActorContainerInterface$NestedActor$Proxy", file.typeSpec.name);
    }

    @Test
    public void generate_nestedInClassName_NestedActor() {
        setTypeElement(ActorContainerClass.NestedActor.class);

        ActorInterfaceGeneratedImpl generated = new ActorInterfaceGeneratedImpl(mDescription);
        JavaFile file = generated.generate(mNamesProvider);

        Assert.assertEquals("com.truecaller.androidactors.cases", file.packageName);
        Assert.assertEquals("ActorContainerClass$NestedActor$Proxy", file.typeSpec.name);
    }

    @Test
    public void getProxyPackage_equals_SimpleActor() {
        setTypeElement(SimpleActor.class);

        ActorInterfaceGeneratedImpl generated = new ActorInterfaceGeneratedImpl(mDescription);
        Assert.assertEquals("com.truecaller.androidactors.cases", generated.getProxyPackage());
    }

    @Test
    public void getProxyName_equals_SimpleActor() {
        setTypeElement(SimpleActor.class);

        ActorInterfaceGeneratedImpl generated = new ActorInterfaceGeneratedImpl(mDescription);
        Assert.assertEquals("SimpleActor$Proxy", generated.getProxyName());
    }

    @Test
    public void generate_validMethods_SimpleActor() {
        setTypeElement(SimpleActor.class);

        List<Method> methods = new ArrayList<>();
        methods.add(method("save",
                argument("key", TypeKind.LONG), argument("value", String.class.getCanonicalName(), true)));
        methods.add(method("get", String.class.getCanonicalName(),
                argument("key", TypeKind.LONG)));
        Mockito.doReturn(methods).when(mDescription).methods();

        ActorInterfaceGeneratedImpl generated = new ActorInterfaceGeneratedImpl(mDescription);
        JavaFile file = generated.generate(mNamesProvider);

        validateSaveMethod(file.typeSpec.methodSpecs);
        validateGetMethod(file.typeSpec.methodSpecs);
    }

    private void validateSaveMethod(final List<MethodSpec> methods) {
        MethodSpec method = Iterables.find(methods, new Predicates.Method("save"));
        Assert.assertTrue(method.hasModifier(Modifier.PUBLIC));
        Assert.assertEquals(TypeName.VOID, method.returnType);
        // Will throw if item does not exist
        Iterables.find(method.annotations, new Predicates.Annotation(Override.class));

        Assert.assertEquals(2, method.parameters.size());

        ParameterSpec param = method.parameters.get(0);
        Assert.assertEquals("key", param.name);
        Assert.assertEquals(TypeName.LONG, param.type);

        param = method.parameters.get(1);
        Assert.assertEquals("value", param.name);
        Assert.assertEquals(ClassName.get(String.class), param.type);
        Iterables.find(param.annotations, new Predicates.Annotation(NonNull.class));
    }

    private void validateGetMethod(final List<MethodSpec> methods) {
        MethodSpec method = Iterables.find(methods, new Predicates.Method("get"));
        Assert.assertTrue(method.hasModifier(Modifier.PUBLIC));
        Assert.assertEquals(ParameterizedTypeName.get(Promise.class, String.class), method.returnType);
        // Will throw if item does not exist
        Iterables.find(method.annotations, new Predicates.Annotation(Override.class));
        Iterables.find(method.annotations, new Predicates.Annotation(NonNull.class));

        Assert.assertEquals(1, method.parameters.size());
        ParameterSpec param = method.parameters.get(0);

        Assert.assertEquals("key", param.name);
        Assert.assertEquals(TypeName.LONG, param.type);
    }
}
