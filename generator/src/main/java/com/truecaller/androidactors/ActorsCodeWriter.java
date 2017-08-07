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

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.JPackage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.OutputStream;

/* package */ class ActorsCodeWriter extends CodeWriter {

    @NotNull
    private final Filer mFiler;

    /* package */ ActorsCodeWriter(@NotNull Filer filer) {
        mFiler = filer;
    }

    @Override
    public OutputStream openBinary(@NotNull JPackage pkg, @NotNull String name) throws IOException {
        String className = extractClassName(name);
        JavaFileObject obj = mFiler.createSourceFile(pkg.name() + "." + className);
        if (obj == null) {
            throw new IOException("Source file for " + name  + " is null");
        }
        return obj.openOutputStream();
    }

    @Override
    public void close() throws IOException {

    }

    private String extractClassName(@NotNull String fileName) throws IOException {
        String path[] = fileName.split("\\.");
        if (path.length != 2) {
            throw new IOException("Invalid class file name: " + fileName);
        }
        return path[0];
    }
}
