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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import java.util.ArrayList;
import java.util.List;

/* package */ class ActorsPackageDescriptionImpl implements ActorsPackageDescription {

    @Nullable
    private final PackageElement mElement;

    private final List<GenerationError> mErrors = new ArrayList<>();

    /* package */ ActorsPackageDescriptionImpl(@NotNull Element element) {
        if (element.getKind() != ElementKind.PACKAGE) {
            mErrors.add(new GenerationError(GenerationError.ER0008, element));
            mElement = null;
            return;
        }

        mElement = (PackageElement) element;
    }

    @Override
    public boolean validate() {
        return mErrors.isEmpty();
    }

    @Override
    public void describeProblems(@NotNull List<GenerationError> errors) {
        errors.addAll(mErrors);
    }

    @NotNull
    @Override
    public String getPackageName() {
        assert mElement != null;
        return mElement.getQualifiedName().toString();
    }

    @NotNull
    @Override
    public String getBuilderClassName() {
        assert mElement != null;
        ActorsPackage annotation = mElement.getAnnotation(ActorsPackage.class);
        return annotation.builderName();
    }

    @NotNull
    @Override
    public ActorsPackage.AccessLevel getAccessLevel() {
        assert mElement != null;
        ActorsPackage annotation = mElement.getAnnotation(ActorsPackage.class);
        return annotation.access();
    }
}
