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

import android.support.annotation.VisibleForTesting;
import com.squareup.javapoet.JavaFile;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Set;

@SupportedAnnotationTypes(value = {"com.truecaller.androidactors.ActorInterface", "com.truecaller.androidactors.ActorsPackage"})
public class ActorsProcessor extends AbstractProcessor {

    private final ModelFactory mModelFactory;

    private ActorParsedModel mParsedModel;

    private ActorGeneratedModel mGeneratedModel;

    @SuppressWarnings("unused")
    public ActorsProcessor() {
        super();
        mModelFactory = new ModelFactoryImpl();
    }

    @VisibleForTesting
    /* package */ ActorsProcessor(@NotNull ModelFactory modelFactory) {
        super();
        mModelFactory = modelFactory;
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mParsedModel = new ActorParsedModel();
        mGeneratedModel = new ActorGeneratedModel();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        configureModel(annotations, mParsedModel);

        parseModel(mParsedModel, roundEnv);

        if (!validateModel(mParsedModel)) {
            for (GenerationError error : mParsedModel.errors) {
                error.print(processingEnv.getMessager());
            }

            return false;
        }

        generateFromModel(mParsedModel, mGeneratedModel);

        return writeModel(mGeneratedModel);
    }

    private void configureModel(@NotNull Set<? extends TypeElement> annotations, @NotNull ActorParsedModel model) {
        model.collectInterfaces = containsAnnotation(annotations, ActorInterface.class);
        model.collectPackages = containsAnnotation(annotations, ActorsPackage.class);
    }

    @VisibleForTesting
    /* package */ void parseModel(@NotNull ActorParsedModel model, @NotNull RoundEnvironment environment) {
        if (model.collectInterfaces) {
            final Set<? extends Element> elements = environment.getElementsAnnotatedWith(ActorInterface.class);
            for (Element element : elements) {
                model.interfaces.add(mModelFactory.createInterfaceDescription(element));
            }
        }

        if (model.collectPackages) {
            Set<? extends Element> packages = environment.getElementsAnnotatedWith(ActorsPackage.class);
            for (Element _package : packages) {
                model.packages.add(mModelFactory.createPackageDescription(_package));
            }
        }
    }

    @VisibleForTesting
    /* package */ boolean validateModel(@NotNull ActorParsedModel model) {
        boolean result = true;

        if (model.collectInterfaces) {
            for (ActorInterfaceDescription _interface : model.interfaces) {
                if (!_interface.validate()) {
                    _interface.describeProblems(model.errors);
                    result = false;
                }
            }
        }

        if (!model.collectInterfaces || model.collectPackages) {
            int packages = 0;
            for (ActorsPackageDescription _package : model.packages) {
                if (!_package.validate()) {
                    _package.describeProblems(model.errors);
                } else {
                    ++packages;
                }
            }

            switch (packages) {
                case 0:
                    model.errors.add(new GenerationError(GenerationError.ER0010));
                    result = false;
                    break;
                case 1:
                    break;
                default:
                    model.errors.add(new GenerationError(GenerationError.ER0009));
                    result = false;
                    break;
            }
        }

        return result;
    }

    @VisibleForTesting
    /* package */ void generateFromModel(@NotNull ActorParsedModel model, @NotNull ActorGeneratedModel generated) {
        if (model.collectInterfaces) {
            for (ActorInterfaceDescription _interface : model.interfaces) {
                generated.interfaces.add(mModelFactory.createInterfaceGenerator(_interface));
            }

            for (ActorInterfaceGenerator _interface : generated.interfaces) {
                generated.files.add(_interface.generate(new NamesProviderImpl()));
            }
        }

        if (!model.collectInterfaces && generated.builder == null) {
            assert model.packages.size() == 1;
            generated.builder = mModelFactory.createBuilderGenerator(model.packages.get(0));
            generated.files.add(generated.builder.generate(generated.interfaces));
        }
    }

    @VisibleForTesting
    /* package */ boolean writeModel(@NotNull ActorGeneratedModel model) {
        try {
            for (JavaFile file : model.files) {
                file.writeTo(processingEnv.getFiler());
            }
            model.files.clear();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
            return false;
        }
        return true;
    }

    private boolean containsAnnotation(Set<? extends TypeElement> annotations, Class<? extends Annotation> needle) {
        for (TypeElement type : annotations) {
            if (needle.getCanonicalName().contentEquals(type.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }
}
