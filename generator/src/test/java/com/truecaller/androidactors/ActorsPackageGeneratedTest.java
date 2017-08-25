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

import com.google.common.collect.Iterables;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

public class ActorsPackageGeneratedTest {

    private static final String PACKAGE_NAME = "com.truecaller.androidactors";
    private static final String BUILDER_CLASS_NAME = "TestPackageGenerator";

    @Mock
    private ActorsPackageDescription mDescription;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Mockito.doReturn(PACKAGE_NAME).when(mDescription).getPackageName();
        Mockito.doReturn(BUILDER_CLASS_NAME).when(mDescription).getBuilderClassName();
        Mockito.doReturn(ActorsPackage.AccessLevel.Package).when(mDescription).getAccessLevel();
    }

    private ActorInterfaceGenerated createProxy(String packageName, String className) {
        ActorInterfaceGenerated _interface = Mockito.mock(ActorInterfaceGenerated.class);
        Mockito.doReturn(packageName).when(_interface).getProxyPackage();
        Mockito.doReturn(className).when(_interface).getProxyName();
        return _interface;
    }

    @Test
    public void generate_validPackageLevel_withProxies() {
        ActorsPackageGenerated generated = new ActorsPackageGenerated(mDescription);
        List<ActorInterfaceGenerated> proxies = new ArrayList<>();
        proxies.add(createProxy("com.truecaller.androidactors", "TestProxy"));
        JavaFile file = generated.generate(proxies);
        Assert.assertEquals(PACKAGE_NAME, file.packageName);

        verifyClass(file.typeSpec, false);

        ClassName self = ClassName.get(file.packageName, file.typeSpec.name);

        TypeSpec cls = file.typeSpec;
        verifyErrorHandlerField(cls.fieldSpecs);
        verifyProxyFactoryField(cls.fieldSpecs);

        verifyErrorHandlerSetter(self, cls.methodSpecs);
        verifyProxyFactorySetter(self, cls.methodSpecs);
        verifyBuildMethod(cls.methodSpecs);

        verifyProxyFactoryClass(cls.typeSpecs);
    }

    @Test
    public void generate_validPublicLevel_withoutProxies() {
        Mockito.doReturn(ActorsPackage.AccessLevel.Public).when(mDescription).getAccessLevel();

        ActorsPackageGenerated generated = new ActorsPackageGenerated(mDescription);
        List<ActorInterfaceGenerated> proxies = new ArrayList<>();
        JavaFile file = generated.generate(proxies);
        Assert.assertEquals(PACKAGE_NAME, file.packageName);

        verifyClass(file.typeSpec, true);

        ClassName self = ClassName.get(file.packageName, file.typeSpec.name);

        TypeSpec cls = file.typeSpec;
        verifyErrorHandlerField(cls.fieldSpecs);
        verifyProxyFactoryField(cls.fieldSpecs);

        verifyErrorHandlerSetter(self, cls.methodSpecs);
        verifyProxyFactorySetter(self, cls.methodSpecs);
        verifyBuildMethod(cls.methodSpecs);

        verifyProxyFactoryClass(cls.typeSpecs);
    }

    private void verifyClass(TypeSpec type, boolean isPublic) {
        Assert.assertNotNull(type);
        Assert.assertEquals(BUILDER_CLASS_NAME, type.name);
        Assert.assertTrue(type.modifiers.contains(Modifier.FINAL));
        if (isPublic) {
            Assert.assertTrue(type.modifiers.contains(Modifier.PUBLIC));
        }
    }

    private void verifyErrorHandlerField(List<FieldSpec> fields) {
        FieldSpec field = Iterables.find(fields, new Predicates.Field(ActorsPackageGenerated.FIELD_ERROR_HANDLER));
        Assert.assertEquals(1, field.modifiers.size());
        Assert.assertTrue(field.modifiers.contains(Modifier.PRIVATE));
        Assert.assertEquals(TypeName.get(FailureHandler.class), field.type);
    }

    private void verifyProxyFactoryField(List<FieldSpec> fields) {
        FieldSpec field = Iterables.find(fields, new Predicates.Field(ActorsPackageGenerated.FIELD_PROXY_FACTORY));

        Assert.assertNotNull(field);
        Assert.assertEquals(1, field.modifiers.size());
        Assert.assertTrue(field.modifiers.contains(Modifier.PRIVATE));
        Assert.assertEquals(TypeName.get(ProxyFactoryBase.class), field.type);
    }

    private void verifyErrorHandlerSetter(TypeName self, List<MethodSpec> methods) {
        MethodSpec method = Iterables.find(methods, new Predicates.Method(ActorsPackageGenerated.METHOD_SET_ERROR_HANDLER));
        Assert.assertEquals(self, method.returnType);
        Assert.assertTrue(method.hasModifier(Modifier.PUBLIC));
        List<ParameterSpec> parameters = method.parameters;
        Assert.assertEquals(1, parameters.size());
        Assert.assertEquals(TypeName.get(FailureHandler.class), parameters.get(0).type);
    }

    private void verifyProxyFactorySetter(TypeName self, List<MethodSpec> methods) {
        MethodSpec method = Iterables.find(methods, new Predicates.Method(ActorsPackageGenerated.METHOD_SET_PROXY_FACTORY));
        Assert.assertEquals(self, method.returnType);
        Assert.assertTrue(method.hasModifier(Modifier.PUBLIC));
        List<ParameterSpec> parameters = method.parameters;
        Assert.assertEquals(1, parameters.size());
        Assert.assertEquals(TypeName.get(ProxyFactoryBase.class), parameters.get(0).type);
    }

    private void verifyBuildMethod(List<MethodSpec> methods) {
        MethodSpec method = Iterables.find(methods, new Predicates.Method(ActorsPackageGenerated.METHOD_BUILD));
        Assert.assertEquals(TypeName.get(ActorsThreads.class), method.returnType);
        Assert.assertTrue(method.hasModifier(Modifier.PUBLIC));
        Assert.assertEquals(0, method.parameters.size());
    }

    private void verifyProxyFactoryClass(List<TypeSpec> types) {
        TypeSpec type = Iterables.find(types, new Predicates.Type(ActorsPackageGenerated.CLASS_PROXY_FACTORY));
        Assert.assertTrue(type.hasModifier(Modifier.PRIVATE));
        Assert.assertTrue(type.hasModifier(Modifier.STATIC));
        Assert.assertEquals(ClassName.get(ProxyFactoryBase.class), type.superclass);
    }
}
