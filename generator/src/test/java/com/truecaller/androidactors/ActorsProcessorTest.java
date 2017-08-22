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

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.io.Writer;
import java.util.HashSet;

public class ActorsProcessorTest {

    @Mock
    private RoundEnvironment mEnvironment;

    @Mock
    private ModelFactory mFactory;

    @Mock
    private ProcessingEnvironment mProcessingEnvironment;

    @Mock
    private Filer mFiler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(mFiler).when(mProcessingEnvironment).getFiler();
    }

    @Test
    public void parseModel_interfacesList_notEmptyActorsInterfaces() {
        ActorInterfaceDescription interface1 = Mockito.mock(ActorInterfaceDescription.class);
        ActorInterfaceDescription interface2 = Mockito.mock(ActorInterfaceDescription.class);
        ActorInterfaceDescription interface3 = Mockito.mock(ActorInterfaceDescription.class);
        Mockito.doReturn(interface1).doReturn(interface2).doReturn(interface3).when(mFactory).createInterfaceDescription(Mockito.<Element>any());

        HashSet<Element> elements = new HashSet<>();
        elements.add(Mockito.mock(Element.class));
        elements.add(Mockito.mock(Element.class));
        elements.add(Mockito.mock(Element.class));
        Mockito.doReturn(elements).when(mEnvironment).getElementsAnnotatedWith(ActorInterface.class);

        ActorParsedModel model = new ActorParsedModel();
        model.collectInterfaces = true;

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        processor.parseModel(model, mEnvironment);

        for (Element element : elements) {
            Mockito.verify(mFactory).createInterfaceDescription(element);
        }
        Mockito.verifyNoMoreInteractions(mFactory);

        Assert.assertSame(interface1, model.interfaces.get(0));
        Assert.assertSame(interface2, model.interfaces.get(1));
        Assert.assertSame(interface3, model.interfaces.get(2));
    }

    @Test
    public void parseModel_packagesList_notEmptyPackages() {
        ActorsPackageDescription package1 = Mockito.mock(ActorsPackageDescription.class);
        Mockito.doReturn(package1).when(mFactory).createPackageDescription(Mockito.<Element>any());

        Element element = Mockito.mock(Element.class);
        HashSet<Element> elements = new HashSet<>();
        elements.add(element);
        Mockito.doReturn(elements).when(mEnvironment).getElementsAnnotatedWith(ActorsPackage.class);

        ActorParsedModel model = new ActorParsedModel();
        model.collectPackages = true;

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        processor.parseModel(model, mEnvironment);

        Mockito.verify(mFactory).createPackageDescription(element);
        Mockito.verifyNoMoreInteractions(mFactory);

        Assert.assertSame(package1, model.packages.get(0));
    }

    @Test
    public void generateFromModel_filesList_notEmptyInterfaces() {
        ActorParsedModel model = new ActorParsedModel();
        ActorGeneratedModel generated = new ActorGeneratedModel();

        model.collectInterfaces = true;
        ActorInterfaceDescription _interface = Mockito.mock(ActorInterfaceDescription.class);
        model.interfaces.add(_interface);

        ActorInterfaceGenerator generator = Mockito.mock(ActorInterfaceGenerator.class);
        JavaFile file = JavaFile.builder("com.truecaller.androidactors", TypeSpec.classBuilder("SimpleActor").build()).build();
        Mockito.doReturn(generator).when(mFactory).createInterfaceGenerator(_interface);
        Mockito.doReturn(file).when(generator).generate(Mockito.<NamesProvider>any());

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        processor.generateFromModel(model, generated);

        Mockito.verify(mFactory).createInterfaceGenerator(_interface);
        Mockito.verify(generator).generate(Mockito.<NamesProvider>any());

        Assert.assertSame(file, generated.files.get(0));

        Mockito.verifyNoMoreInteractions(mFactory);
    }

    @Test
    public void generateFromModel_filesList_builder() {
        ActorParsedModel model = new ActorParsedModel();
        ActorGeneratedModel generated = new ActorGeneratedModel();

        ActorInterfaceGenerator generator = Mockito.mock(ActorInterfaceGenerator.class);
        generated.interfaces.add(generator);

        ActorsPackageDescription packageDescription = Mockito.mock(ActorsPackageDescription.class);
        model.packages.add(packageDescription);

        ActorsPackageGenerated packageGenerated = Mockito.mock(ActorsPackageGenerated.class);
        Mockito.doReturn(packageGenerated).when(mFactory).createBuilderGenerator(packageDescription);
        JavaFile file = JavaFile.builder("com.truecaller.androidactors", TypeSpec.classBuilder("SimpleActor").build()).build();
        Mockito.doReturn(file).when(packageGenerated).generate(Mockito.anyListOf(ActorInterfaceGenerator.class));

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        processor.generateFromModel(model, generated);

        Assert.assertEquals(1, model.packages.size());
        Assert.assertSame(file, generated.files.get(0));
    }

    @Test
    public void generateFromModel_doNothing_builder() {
        ActorParsedModel model = new ActorParsedModel();
        ActorGeneratedModel generated = new ActorGeneratedModel();

        ActorInterfaceGenerator generator = Mockito.mock(ActorInterfaceGenerator.class);
        generated.interfaces.add(generator);
        generated.builder = Mockito.mock(ActorsPackageGenerated.class);

        ActorsPackageDescription packageDescription = Mockito.mock(ActorsPackageDescription.class);
        model.packages.add(packageDescription);

        ActorsPackageGenerated packageGenerated = Mockito.mock(ActorsPackageGenerated.class);
        Mockito.doReturn(packageGenerated).when(mFactory).createBuilderGenerator(packageDescription);

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        processor.generateFromModel(model, generated);

        Mockito.verifyZeroInteractions(mFactory);
        Assert.assertEquals(0, generated.files.size());
    }

    @Test
    public void validateModel_false_noPackageAnnotations() {
        ActorParsedModel model = new ActorParsedModel();

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        Assert.assertEquals(false, processor.validateModel(model));

        Assert.assertEquals(1, model.errors.size());
        Assert.assertEquals(true, model.errors.get(0).isError);
        Assert.assertEquals(GenerationError.ER0010, model.errors.get(0).message);
    }

    @Test
    public void validateModel_false_moreThanOnePackageAnnotations() {
        ActorParsedModel model = new ActorParsedModel();
        ActorsPackageDescription packageDescription = Mockito.mock(ActorsPackageDescription.class);
        Mockito.doReturn(true).when(packageDescription).validate();

        model.packages.add(packageDescription);
        model.packages.add(packageDescription);

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        Assert.assertEquals(false, processor.validateModel(model));

        Assert.assertEquals(1, model.errors.size());
        Assert.assertEquals(true, model.errors.get(0).isError);
        Assert.assertEquals(GenerationError.ER0009, model.errors.get(0).message);
    }

    @Test
    public void validateModel_false_nonValidPackage() {
        ActorParsedModel model = new ActorParsedModel();
        ActorsPackageDescription packageDescription = Mockito.mock(ActorsPackageDescription.class);
        Mockito.doReturn(false).when(packageDescription).validate();

        model.packages.add(packageDescription);

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        Assert.assertEquals(false, processor.validateModel(model));
    }

    @Test
    public void validateModel_true_oneValidPackage() {
        ActorParsedModel model = new ActorParsedModel();
        ActorsPackageDescription packageDescription = Mockito.mock(ActorsPackageDescription.class);
        Mockito.doReturn(true).when(packageDescription).validate();

        model.packages.add(packageDescription);

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        Assert.assertEquals(true, processor.validateModel(model));
    }

    @Test
    public void validateModel_true_validInterfaceAndCollectInterfaces() {
        ActorParsedModel model = new ActorParsedModel();
        ActorInterfaceDescription _interface = Mockito.mock(ActorInterfaceDescription.class);
        Mockito.doReturn(true).when(_interface).validate();
        model.collectInterfaces = true;
        model.interfaces.add(_interface);

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        Assert.assertEquals(true, processor.validateModel(model));
    }

    @Test
    public void validateModel_false_notValidInterfaceAndCollectInterfaces() {
        ActorParsedModel model = new ActorParsedModel();
        ActorInterfaceDescription _interface = Mockito.mock(ActorInterfaceDescription.class);
        Mockito.doReturn(false).when(_interface).validate();
        model.collectInterfaces = true;
        model.interfaces.add(_interface);

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        Assert.assertEquals(false, processor.validateModel(model));
        Mockito.verify(_interface).describeProblems(Mockito.anyListOf(GenerationError.class));
    }

    @Test
    public void writeModel_writeAndClean_filesList() throws Exception {
        ActorGeneratedModel model = new ActorGeneratedModel();
        JavaFile file = JavaFile.builder("com.truecaller.androidactors", TypeSpec.classBuilder("SimpleActor").build()).build();
        model.files.add(file);

        JavaFileObject javaFileObject = Mockito.mock(JavaFileObject.class);
        Writer writer = Mockito.mock(Writer.class);
        Mockito.doReturn(writer).when(javaFileObject).openWriter();
        Mockito.doReturn(javaFileObject).when(mFiler).createSourceFile(Mockito.anyString(), Mockito.<Element>anyVararg());

        ActorsProcessor processor = new ActorsProcessor(mFactory);
        processor.init(mProcessingEnvironment);
        processor.writeModel(model);

        Mockito.verify(mFiler).createSourceFile(Mockito.eq("com.truecaller.androidactors.SimpleActor"), Mockito.<Element>anyVararg());
        Assert.assertEquals(0, model.files.size());
    }
}
