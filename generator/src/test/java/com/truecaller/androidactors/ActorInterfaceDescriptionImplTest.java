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
import com.google.testing.compile.CompilationRule;
import com.truecaller.androidactors.cases.ActorClass;
import com.truecaller.androidactors.cases.ActorGenerifiedPromise;
import com.truecaller.androidactors.cases.ActorNullablePromise;
import com.truecaller.androidactors.cases.ActorWithConstant;
import com.truecaller.androidactors.cases.ActorWithException;
import com.truecaller.androidactors.cases.ActorWithGenericMethod;
import com.truecaller.androidactors.cases.ActorWithNonPromise;
import com.truecaller.androidactors.cases.PrivateActorContainer;
import com.truecaller.androidactors.cases.SimpleActor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleElementVisitor6;
import java.util.ArrayList;
import java.util.List;

public class ActorInterfaceDescriptionImplTest {

    @Rule
    public final CompilationRule rule = new CompilationRule();

    private TypeElement getTypeElement(@NotNull Class cls) {
        return getTypeElement(cls.getCanonicalName());
    }

    private TypeElement getTypeElement(@NotNull String className) {
        return rule.getElements().getTypeElement(className);
    }

    @Nullable
    private ExecutableElement getMethod(@NotNull TypeElement type, @NotNull String name) {
        for (Element element : type.getEnclosedElements()) {
            ExecutableElement result = element.accept(new SimpleElementVisitor6<ExecutableElement, String>() {
                @Override
                public ExecutableElement visitExecutable(ExecutableElement e, String s) {
                    if (s.contentEquals(e.getSimpleName())) {
                        return e;
                    }
                    return null;
                }
            }, name);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    @Test
    public void validate_false_notInterface() {
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(getTypeElement(ActorClass.class));
        Assert.assertEquals(false, description.validate());
    }

    @Test
    public void describeProblems_ER0001_notInterface() {
        TypeElement element = getTypeElement(ActorClass.class);

        List<GenerationError> errors = new ArrayList<>();
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);

        description.describeProblems(errors);

        Assert.assertEquals(1, errors.size());

        Assert.assertEquals(true, errors.get(0).isError);
        Assert.assertEquals(GenerationError.ER0001, errors.get(0).message);
        Assert.assertSame(element, errors.get(0).element);
    }

    @Test
    public void validate_false_privateInterface() {
        TypeElement element = getTypeElement(PrivateActorContainer.getActorClassName());
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        Assert.assertEquals(false, description.validate());
    }

    @Test
    public void describeProblems_ER0002_privateInterface() {
        TypeElement element = getTypeElement(PrivateActorContainer.getActorClassName());

        List<GenerationError> errors = new ArrayList<>();
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);

        description.describeProblems(errors);

        Assert.assertEquals(1, errors.size());

        Assert.assertEquals(true, errors.get(0).isError);
        Assert.assertEquals(GenerationError.ER0002, errors.get(0).message);
        Assert.assertSame(element, errors.get(0).element);
    }

    @Test
    public void validate_false_methodThrows() {
        TypeElement element = getTypeElement(ActorWithException.class);
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        Assert.assertEquals(false, description.validate());
    }

    @Test
    public void describeProblems_ER0003_methodThrows() {
        TypeElement element = getTypeElement(ActorWithException.class);

        List<GenerationError> errors = new ArrayList<>();
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        description.validate();

        description.describeProblems(errors);
        Assert.assertEquals(true, errors.get(0).isError);
        Assert.assertEquals(GenerationError.ER0003, errors.get(0).message);
        Assert.assertSame(getMethod(element, "testMethod"), errors.get(0).element);
    }

    @Test
    public void validate_false_genericMethod() {
        TypeElement element = getTypeElement(ActorWithGenericMethod.class);
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        Assert.assertEquals(false, description.validate());
    }

    @Test
    public void describeProblems_ER0004_genericMethod() {
        TypeElement element = getTypeElement(ActorWithGenericMethod.class);

        List<GenerationError> errors = new ArrayList<>();
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        description.validate();

        description.describeProblems(errors);
        Assert.assertEquals(true, errors.get(0).isError);
        Assert.assertEquals(GenerationError.ER0004, errors.get(0).message);
        Assert.assertSame(getMethod(element, "saveValue"), errors.get(0).element);
    }

    @Test
    public void validate_false_returnPrimitiveValue() {
        TypeElement element = getTypeElement(ActorWithNonPromise.class);
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        Assert.assertEquals(false, description.validate());
    }

    @Test
    public void describeProblems_ER0005_returnPrimitiveValue() {
        TypeElement element = getTypeElement(ActorWithNonPromise.class);

        List<GenerationError> errors = new ArrayList<>();
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        description.validate();

        description.describeProblems(errors);
        Assert.assertEquals(true, errors.get(0).isError);
        Assert.assertEquals(GenerationError.ER0005, errors.get(0).message);
        Assert.assertSame(getMethod(element, "testMethod"), errors.get(0).element);
    }


    @Test
    public void validate_false_returnNullablePromise() {
        TypeElement element = getTypeElement(ActorNullablePromise.class);
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        Assert.assertEquals(false, description.validate());
    }

    @Test
    public void describeProblems_ER0006_returnNullablePromise() {
        TypeElement element = getTypeElement(ActorNullablePromise.class);

        List<GenerationError> errors = new ArrayList<>();
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        description.validate();

        description.describeProblems(errors);
        Assert.assertEquals(true, errors.get(0).isError);
        Assert.assertEquals(GenerationError.ER0006, errors.get(0).message);
        Assert.assertSame(getMethod(element, "testMethod"), errors.get(0).element);
    }

    @Test
    public void validate_false_returnNonSpecifiedPromise() {
        TypeElement element = getTypeElement(ActorGenerifiedPromise.class);
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        Assert.assertEquals(false, description.validate());
    }

    @Test
    public void describeProblems_ER0007_returnNonSpecifiedPromise() {
        TypeElement element = getTypeElement(ActorGenerifiedPromise.class);

        List<GenerationError> errors = new ArrayList<>();
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        description.validate();

        description.describeProblems(errors);
        Assert.assertEquals(true, errors.get(0).isError);
        Assert.assertEquals(GenerationError.ER0007, errors.get(0).message);
        Assert.assertSame(getMethod(element, "testMethod"), errors.get(0).element);
    }

    @Test
    public void validate_true_withConstant() {
        TypeElement element = getTypeElement(ActorWithConstant.class);
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        Assert.assertEquals(true, description.validate());
    }

    @Test
    public void validate_true_simpleActor() {
        TypeElement element = getTypeElement(SimpleActor.class);
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        Assert.assertEquals(true, description.validate());
    }

    @Test
    public void getType_same_simpleActor() {
        TypeElement element = getTypeElement(SimpleActor.class);
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);
        Assert.assertSame(element, description.getType());
    }

    @Test
    public void methods_same_simpleActor() {
        TypeElement element = getTypeElement(SimpleActor.class);
        ActorInterfaceDescriptionImpl description = new ActorInterfaceDescriptionImpl(element);

        ActorInterfaceDescription.Method[] methods = Iterables.toArray(description.methods(), ActorInterfaceDescription.Method.class);

        Assert.assertEquals(2, methods.length);

        validateMethod(getMethod(element, "save"), null, methods[0]);
        validateMethod(getMethod(element, "get"), getTypeElement(String.class).asType(), methods[1]);
    }

    private void validateMethod(ExecutableElement expected, TypeMirror promisedType, ActorInterfaceDescription.Method actual) {
        Assert.assertEquals(expected.getSimpleName().toString(), actual.getName());
        assertSameType(promisedType, actual.getPromisedType());
        assertSameType(expected.getReturnType(), actual.getType());

        List<? extends VariableElement> expectedArgs = expected.getParameters();
        ActorInterfaceDescription.Method.Argument arguments[] = Iterables.toArray(actual.arguments(), ActorInterfaceDescription.Method.Argument.class);

        Assert.assertEquals(expectedArgs.size(), arguments.length);
        for (int index = 0; index < arguments.length; ++index) {
            Assert.assertEquals(expectedArgs.get(index).getSimpleName().toString(), arguments[index].getName());
            assertSameType(expectedArgs.get(index).asType(), arguments[index].getType());
        }
    }

    private void assertSameType(TypeMirror expected, TypeMirror actual) {
        if (expected == null) {
            Assert.assertEquals(expected, actual);
            return;
        }

        Assert.assertEquals(true, rule.getTypes().isSameType(expected, actual));
    }
}
