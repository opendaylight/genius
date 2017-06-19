/*
 * Copyright (c) 2017 Ericsson, S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import java.io.Closeable;
import java.util.Collection;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.yangtools.yang.binding.DataObject;

/**
 * Interface to be implemented by classes interested in receiving notifications
 * about data tree changes. It implements a default method to handle the data
 * tree modifications. Those notifications will be forwarded to the appropriate
 * methods (add, update, remove) depending on their action type. The listeners
 * implementing this interface will need to be annotated as {@link Singleton}.
 *
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 *
 * @param <T>
 *            type of the data object the listener is registered to.
 */
@Singleton
public interface DataTreeChangeListenerActions<T extends DataObject> extends Closeable {

    /**
     * Default method invoked upon data tree change, in turn it calls the
     * appropriate method (add, update, remove) depending on the type of change.
     *
     * @param collection
     *            collection of changes
     */
    default void onDataTreeChanged(@Nonnull Collection<DataTreeModification<T>> collection) {
        for (final DataTreeModification<T> dataTreeModification : collection) {
            final DataObjectModification<T> dataObjectModification = dataTreeModification.getRootNode();
            switch (dataObjectModification.getModificationType()) {
                case SUBTREE_MODIFIED:
                    update(dataObjectModification.getDataBefore(), dataObjectModification.getDataAfter());
                    break;
                case DELETE:
                    remove(dataObjectModification.getDataBefore());
                    break;
                case WRITE:
                    if (dataObjectModification.getDataBefore() == null) {
                        add(dataObjectModification.getDataAfter());
                    } else {
                        update(dataObjectModification.getDataBefore(), dataObjectModification.getDataAfter());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Invoked when new data object added.
     *
     * @param newDataObject
     *            newly added object
     */
    void add(@Nonnull T newDataObject);

    /**
     * Invoked when the data object has been removed.
     *
     * @param removedDataObject
     *            existing object being removed
     */
    void remove(@Nonnull T removedDataObject);

    /**
     * Invoked when there is a change in the data object.
     *
     * @param originalDataObject
     *            existing object being modified
     * @param updatedDataObject
     *            modified data object
     */
    void update(@Nonnull T originalDataObject, T updatedDataObject);
}
