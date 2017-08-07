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
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TypeUtilsTest {

    private final JCodeModel model = new JCodeModel();

    @Mock
    private TypeElement typeElement;

    @Mock
    private VariableElement variableElement;

    @Mock
    private NoType noType;

    @Mock
    private PrimitiveType primitiveType;

    @Mock
    private ArrayType arrayType;

    @Mock
    private DeclaredType declaredType;

    @Mock
    private Name typeName;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Mockito.doReturn(TypeKind.VOID).when(noType).getKind();

        Mockito.doReturn(TypeKind.ARRAY).when(arrayType).getKind();

        Mockito.doReturn(TypeKind.DECLARED).when(declaredType).getKind();
        Mockito.doReturn(typeElement).when(declaredType).asElement();
        Mockito.doReturn(typeName).when(typeElement).getQualifiedName();
    }

    @Test
    public void testIsPublic() {
        final TypeUtils utils = new TypeUtils(model);
        final Set<Modifier> modifiers = new HashSet<>();
        Mockito.doReturn(modifiers).when(typeElement).getModifiers();

        // /* package */ class A {};
        Assert.assertFalse(utils.isPublic(typeElement));

        // private final class A {};
        modifiers.clear();
        modifiers.add(Modifier.FINAL);
        modifiers.add(Modifier.PRIVATE);
        Assert.assertFalse(utils.isPublic(typeElement));

        // public abstract class A {};
        modifiers.clear();
        modifiers.add(Modifier.ABSTRACT);
        modifiers.add(Modifier.PUBLIC);
        Assert.assertTrue(utils.isPublic(typeElement));

        // public class A {};
        modifiers.clear();
        modifiers.add(Modifier.PUBLIC);
        Assert.assertTrue(utils.isPublic(typeElement));
    }

    @Test
    public void testIsNotNull() {
        final TypeUtils utils = new TypeUtils(model);

        Mockito.doReturn(null).when(variableElement).getAnnotation(NonNull.class);
        Assert.assertFalse(utils.isNotNull(variableElement));
        Mockito.verify(variableElement).getAnnotation(NonNull.class);

        Mockito.doReturn(Mockito.mock(NonNull.class)).when(variableElement).getAnnotation(NonNull.class);
        Assert.assertTrue(utils.isNotNull(variableElement));
    }

    @Test
    public void testVoidToJType() {
        final TypeUtils utils = new TypeUtils(model);
        Assert.assertSame(model.VOID, utils.toJType(noType));
    }

    @Test
    public void testPrimitiveToJType() {
        final TypeUtils utils = new TypeUtils(model);

        // boolean
        Mockito.doReturn(TypeKind.BOOLEAN).when(primitiveType).getKind();
        Assert.assertSame(model.BOOLEAN, utils.toJType(primitiveType));

        // byte
        Mockito.doReturn(TypeKind.BYTE).when(primitiveType).getKind();
        Assert.assertSame(model.BYTE, utils.toJType(primitiveType));

        // short
        Mockito.doReturn(TypeKind.SHORT).when(primitiveType).getKind();
        Assert.assertSame(model.SHORT, utils.toJType(primitiveType));

        // int
        Mockito.doReturn(TypeKind.INT).when(primitiveType).getKind();
        Assert.assertSame(model.INT, utils.toJType(primitiveType));

        // long
        Mockito.doReturn(TypeKind.LONG).when(primitiveType).getKind();
        Assert.assertSame(model.LONG, utils.toJType(primitiveType));

        // char
        Mockito.doReturn(TypeKind.CHAR).when(primitiveType).getKind();
        Assert.assertSame(model.CHAR, utils.toJType(primitiveType));

        // float
        Mockito.doReturn(TypeKind.FLOAT).when(primitiveType).getKind();
        Assert.assertSame(model.FLOAT, utils.toJType(primitiveType));

        // double
        Mockito.doReturn(TypeKind.DOUBLE).when(primitiveType).getKind();
        Assert.assertSame(model.DOUBLE, utils.toJType(primitiveType));
    }

    @Test
    public void testPrimitiveArrayToJType() {
        final TypeUtils utils = new TypeUtils(model);
        Mockito.doReturn(primitiveType).when(arrayType).getComponentType();

        // boolean
        Mockito.doReturn(TypeKind.BOOLEAN).when(primitiveType).getKind();
        JType result = utils.toJType(arrayType);
        Assert.assertTrue(result.isArray());
        Assert.assertSame(model.BOOLEAN, result.elementType());

        // byte
        Mockito.doReturn(TypeKind.BYTE).when(primitiveType).getKind();
        result = utils.toJType(arrayType);
        Assert.assertTrue(result.isArray());
        Assert.assertSame(model.BYTE, result.elementType());

        // short
        Mockito.doReturn(TypeKind.SHORT).when(primitiveType).getKind();
        result = utils.toJType(arrayType);
        Assert.assertTrue(result.isArray());
        Assert.assertSame(model.SHORT, result.elementType());

        // int
        Mockito.doReturn(TypeKind.INT).when(primitiveType).getKind();
        result = utils.toJType(arrayType);
        Assert.assertTrue(result.isArray());
        Assert.assertSame(model.INT, result.elementType());

        // long
        Mockito.doReturn(TypeKind.LONG).when(primitiveType).getKind();
        result = utils.toJType(arrayType);
        Assert.assertTrue(result.isArray());
        Assert.assertSame(model.LONG, result.elementType());

        // char
        Mockito.doReturn(TypeKind.CHAR).when(primitiveType).getKind();
        result = utils.toJType(arrayType);
        Assert.assertTrue(result.isArray());
        Assert.assertSame(model.CHAR, result.elementType());

        // float
        Mockito.doReturn(TypeKind.FLOAT).when(primitiveType).getKind();
        result = utils.toJType(arrayType);
        Assert.assertTrue(result.isArray());
        Assert.assertSame(model.FLOAT, result.elementType());

        // double
        Mockito.doReturn(TypeKind.DOUBLE).when(primitiveType).getKind();
        result = utils.toJType(arrayType);
        Assert.assertTrue(result.isArray());
        Assert.assertSame(model.DOUBLE, result.elementType());
    }

    @Test
    public void testSimpleDeclaredToJType() {
        final TypeUtils utils = new TypeUtils(model);
        Mockito.doReturn("java.lang.String").when(typeName).toString();

        JType result = utils.toJType(declaredType);
        Assert.assertTrue(result.isReference());
        Assert.assertEquals("java.lang.String", result.binaryName());
    }

    @Test
    public void testSimpleDeclaredArrayToJType() {
        final TypeUtils utils = new TypeUtils(model);
        Mockito.doReturn("java.lang.String").when(typeName).toString();
        Mockito.doReturn(declaredType).when(arrayType).getComponentType();

        JType result = utils.toJType(arrayType);
        Assert.assertTrue(result.isArray());
        result = result.elementType();
        Assert.assertTrue(result.isReference());
        Assert.assertEquals("java.lang.String", result.binaryName());

        // Same but empty params
        Mockito.doReturn("java.lang.Set").when(typeName).toString();
        List<TypeMirror> types = new ArrayList<>();
        Mockito.doReturn(types).when(declaredType).getTypeArguments();
        result = utils.toJType(declaredType);
        Assert.assertTrue(result.isReference());
        Assert.assertEquals("java.lang.Set", result.erasure().binaryName());
    }

    @Test
    public void testSimpleGenericDeclaredToJType() {
        final TypeUtils utils = new TypeUtils(model);
        final DeclaredType stringType = Mockito.mock(DeclaredType.class);
        final TypeElement stringTypeElement = Mockito.mock(TypeElement.class);
        Name stringTypeName = Mockito.mock(Name.class);

        Mockito.doReturn(TypeKind.DECLARED).when(stringType).getKind();
        Mockito.doReturn(stringTypeElement).when(stringType).asElement();
        Mockito.doReturn(stringTypeName).when(stringTypeElement).getQualifiedName();
        Mockito.doReturn("java.lang.String").when(stringTypeName).toString();

        Mockito.doReturn("java.lang.Set").when(typeName).toString();
        List<TypeMirror> types = new ArrayList<>();
        types.add(stringType);
        Mockito.doReturn(types).when(declaredType).getTypeArguments();

        JType result = utils.toJType(declaredType);
        Assert.assertTrue(result.isReference());
        Assert.assertEquals("java.lang.Set", result.erasure().binaryName());
        Assert.assertTrue(result instanceof JClass);
        List<JClass> params = ((JClass) result).getTypeParameters();
        Assert.assertNotNull(params);
        Assert.assertEquals(1, params.size());
        Assert.assertEquals("java.lang.String", params.get(0).binaryName());
    }
}
