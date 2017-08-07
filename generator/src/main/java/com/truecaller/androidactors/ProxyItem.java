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

import com.sun.codemodel.JDefinedClass;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.TypeElement;

/* package */ class ProxyItem {
    final TypeElement type;
    final JDefinedClass proxyClass;

    ProxyItem(@NotNull TypeElement type, @NotNull JDefinedClass proxyClass) {
        this.type = type;
        this.proxyClass = proxyClass;
    }

    @Override
    public int hashCode() {
        return type.getQualifiedName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ProxyItem &&
                type.getQualifiedName().equals(((ProxyItem) obj).type.getQualifiedName());
    }
}
