/*
 * Copyright (c) 2019 PANTHEON.tech, s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.batching;

import static java.util.Objects.requireNonNull;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.concepts.Identifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Utility class for creating ActionableResource instances.
 */
public final class ActionableResources {
    private ActionableResources() {

    }

    public static <T extends DataObject> @NonNull ActionableResource create(final InstanceIdentifier<T> path,
            final T data) {
        return new ActionableResourceImpl(path, ActionableResource.CREATE, requireNonNull(data), null);
    }

    public static <T extends DataObject> @NonNull ActionableResource create(final Identifier identifier,
            final InstanceIdentifier<T> path, final T data) {
        return new ActionableResourceImpl(identifier, path, ActionableResource.CREATE, requireNonNull(data), null);
    }

    public static <T extends DataObject> @NonNull ActionableResource update(final InstanceIdentifier<T> path,
            final T newData) {
        return new ActionableResourceImpl(path, ActionableResource.UPDATE, requireNonNull(newData), null);
    }

    public static <T extends DataObject> @NonNull ActionableResource update(final Identifier identifier,
            final InstanceIdentifier<T> path, final T newData, final T oldData) {
        return new ActionableResourceImpl(identifier, path, ActionableResource.UPDATE, requireNonNull(newData),
            oldData);
    }

    public static @NonNull ActionableResource delete(final InstanceIdentifier<?> path) {
        return new ActionableResourceImpl(path, ActionableResource.DELETE, null, null);
    }

    public static <T extends DataObject> @NonNull ActionableResource delete(final Identifier identifier,
            final InstanceIdentifier<T> path, final T data) {
        return new ActionableResourceImpl(identifier, path, ActionableResource.DELETE, data, null);
    }
}
