/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Utility for Java types.
 *
 * <p>see also <a href="https://github.com/google/guava/wiki/ReflectionExplained">Guava's TypeToken</a>
 *
 * @author Stephen Kitt
 */
public final class SuperTypeUtil {

    private SuperTypeUtil() {

    }

    @SuppressWarnings("unchecked")
    public static <U> Class<U> getTypeParameter(Class<?> klass, int index) {
        Type superclass = klass.getGenericSuperclass();
        if (superclass instanceof Class) {
            throw new IllegalStateException("Missing type parameters");
        }
        ParameterizedType parameterizedType = (ParameterizedType) superclass;
        if (parameterizedType.getActualTypeArguments().length < index) {
            throw new IllegalStateException("Missing type parameters");
        }
        return (Class<U>) parameterizedType.getActualTypeArguments()[index];
    }
}
