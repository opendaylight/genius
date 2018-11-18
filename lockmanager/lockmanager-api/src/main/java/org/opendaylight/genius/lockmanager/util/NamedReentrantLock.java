/*
 * Copyright (c) 2018 Pantheon Technologies, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.lockmanager.util;

import static java.util.Objects.requireNonNull;

import com.google.common.base.MoreObjects;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Identifiable;

final class NamedReentrantLock<T> extends ReentrantLock implements Identifiable<T> {
    private static final long serialVersionUID = 1L;

    private final @NonNull T name;

    NamedReentrantLock(final T name) {
        super();
        this.name = requireNonNull(name);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("name", name).toString();
    }

    @Override
    public @NonNull T getIdentifier() {
        return name;
    }
}
