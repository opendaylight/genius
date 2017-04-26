/*
 * Copyright (c) 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.databrokerutils.internal;

import java.util.Optional;

/**
 * Convert from Guava to/from Java 8 Optional.
 *
 * @author Michael Vorburger.ch
 */
public class Optionals {

    // http://stackoverflow.com/questions/33918490/optional-conversion-from-guava-to-java

    // NB this ^^^ seems to suggest that this VVV is more optimal than Guava 21 new built-in toJavaUtil and fromJavaUtil

    static <T> Optional<T> toJavaUtil(com.google.common.base.Optional<T> guavaOptional) {
        return guavaOptional.transform(java.util.Optional::of).or(java.util.Optional.empty());
    }

}
