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

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JPackage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;

public class CodeWritersTest {

    private static final String PACKAGE_NAME = "com.truecaller.androidactors.test";

    @Mock
    private Filer filer;

    private JPackage pkg;

    @Mock
    private FileObject resourceFile;

    @Mock
    private OutputStream resourceStream;

    @Mock
    private JavaFileObject sourceFile;

    @Mock
    private OutputStream sourceStream;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        JCodeModel model = new JCodeModel();
        pkg = model._package(PACKAGE_NAME);

        Mockito.doReturn(resourceStream).when(resourceFile).openOutputStream();
        Mockito.doReturn(resourceFile).when(filer).createResource(Mockito.<JavaFileManager.Location>any(),
                Mockito.<CharSequence>any(), Mockito.<CharSequence>any(), Mockito.<Element>anyVararg());

        Mockito.doReturn(sourceStream).when(sourceFile).openOutputStream();
        Mockito.doReturn(sourceFile).when(filer).createSourceFile(Mockito.<CharSequence>any(), Mockito.<Element>anyVararg());
    }

    @Test
    public void testResourceWriter() throws Exception {
        ResourcesCodeWriter writer = new ResourcesCodeWriter(filer);
        OutputStream stream = writer.openBinary(pkg, "resource.xml");
        Assert.assertSame(resourceStream, stream);
        Mockito.verify(filer).createResource(StandardLocation.SOURCE_OUTPUT, PACKAGE_NAME, "resource.xml");
    }

    @Test(expected = IOException.class)
    public void testResourceNullFile() throws Exception {
        Mockito.doReturn(null).when(filer).createResource(Mockito.<JavaFileManager.Location>any(),
                Mockito.<CharSequence>any(), Mockito.<CharSequence>any(), Mockito.<Element>anyVararg());

        ResourcesCodeWriter writer = new ResourcesCodeWriter(filer);
        // must throw IOException
        writer.openBinary(pkg, "resource.xml");
    }

    @Test(expected = IOException.class)
    public void testResourceIoException() throws Exception {
        Mockito.doThrow(new IOException()).when(resourceFile).openOutputStream();

        ResourcesCodeWriter writer = new ResourcesCodeWriter(filer);
        // must throw IOException
        writer.openBinary(pkg, "resource.xml");
    }

    @Test
    public void testSourceWriter() throws Exception {
        ActorsCodeWriter writer = new ActorsCodeWriter(filer);
        OutputStream stream = writer.openBinary(pkg, "Actor.java");
        Assert.assertSame(sourceStream, stream);
        Mockito.verify(filer).createSourceFile(PACKAGE_NAME + ".Actor");
    }

    @Test(expected = IOException.class)
    public void testSourceNullFile() throws Exception {
        Mockito.doReturn(null).when(filer).createSourceFile(Mockito.<CharSequence>any(), Mockito.<Element>anyVararg());

        ActorsCodeWriter writer = new ActorsCodeWriter(filer);
        // must throw IOException
        writer.openBinary(pkg, "Actor.java");
    }

    @Test(expected = IOException.class)
    public void testSourceIoException() throws Exception {
        Mockito.doThrow(new IOException()).when(sourceFile).openOutputStream();

        ActorsCodeWriter writer = new ActorsCodeWriter(filer);
        // must throw IOException
        writer.openBinary(pkg, "Actor.java");
    }

    @Test(expected = IOException.class)
    public void testSourceInvalidFileName() throws Exception {
        ActorsCodeWriter writer = new ActorsCodeWriter(filer);
        // must throw IOException
        writer.openBinary(pkg, "Actor.Simple.java");
    }
}
