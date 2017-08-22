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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import java.util.ArrayList;
import java.util.List;

public class ActorsPackageDescriptionImplTest {

    private static final String PACKAGE_NAME = "com.truecaller.androidactors";

    private static final String BUILDER_CLASS_NAME = "TestActorsBuilder";

    @Mock
    private PackageElement mElement;

    @Mock
    private ActorsPackage mAnnotation;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Mockito.doReturn(BUILDER_CLASS_NAME).when(mAnnotation).builderName();
        Mockito.doReturn(ActorsPackage.AccessLevel.Package).when(mAnnotation).access();

        Mockito.doReturn(ElementKind.PACKAGE).when(mElement).getKind();
        Mockito.doReturn(new NameMock(PACKAGE_NAME)).when(mElement).getQualifiedName();
        Mockito.doReturn(mAnnotation).when(mElement).getAnnotation(ActorsPackage.class);
    }

    @Test
    public void validate_false_notPackage() {
        Mockito.doReturn(ElementKind.INTERFACE).when(mElement).getKind();

        ActorsPackageDescriptionImpl impl = new ActorsPackageDescriptionImpl(mElement);
        Assert.assertEquals(false, impl.validate());
    }

    @Test
    public void describeProblems_ER0008_notPackage() {
        Mockito.doReturn(ElementKind.INTERFACE).when(mElement).getKind();

        List<GenerationError> errors = new ArrayList<>();
        ActorsPackageDescriptionImpl impl = new ActorsPackageDescriptionImpl(mElement);
        impl.describeProblems(errors);

        Assert.assertEquals(true, errors.get(0).isError);
        Assert.assertEquals(GenerationError.ER0008, errors.get(0).message);
        Assert.assertSame(mElement, errors.get(0).element);
    }

    @Test
    public void validate_true_package() {
        ActorsPackageDescriptionImpl impl = new ActorsPackageDescriptionImpl(mElement);
        Assert.assertEquals(true, impl.validate());
    }

    @Test
    public void describeProblems_noError_package() {
        List<GenerationError> errors = new ArrayList<>();
        ActorsPackageDescriptionImpl impl = new ActorsPackageDescriptionImpl(mElement);
        impl.describeProblems(errors);
        Assert.assertEquals(0, errors.size());
    }

    @Test
    public void getPackageName_sameObject_providePackage() {
        ActorsPackageDescriptionImpl impl = new ActorsPackageDescriptionImpl(mElement);
        Assert.assertEquals(PACKAGE_NAME, impl.getPackageName());
    }

    @Test
    public void getBuilderClassName_sameObject_provideConfiguredAnnotation() {
        ActorsPackageDescriptionImpl impl = new ActorsPackageDescriptionImpl(mElement);
        Assert.assertEquals(BUILDER_CLASS_NAME, impl.getBuilderClassName());
    }

    @Test
    public void getAccessLevel_sameObject_provideConfiguredAnnotation() {
        ActorsPackageDescriptionImpl impl = new ActorsPackageDescriptionImpl(mElement);
        Assert.assertEquals(ActorsPackage.AccessLevel.Package, impl.getAccessLevel());
    }
}
