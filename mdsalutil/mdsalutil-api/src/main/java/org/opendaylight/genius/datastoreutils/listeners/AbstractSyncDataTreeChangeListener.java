/*
 * Copyright (c) 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract class providing some common functionality to specific listeners.
 *
 * @param <T> type of the data object the listener is registered to.
 * @author David Suárez (david.suarez.fuentes@gmail.com)
 */
public abstract class AbstractSyncDataTreeChangeListener<T extends DataObject> extends
        AbstractDataTreeChangeListener<T> implements DataTreeChangeListenerActions<T>, DataTreeChangeListener<T> {

    @Inject
    public AbstractSyncDataTreeChangeListener(DataBroker dataBroker, DataTreeIdentifier<T> dataTreeIdentifier) {
        super(dataBroker, dataTreeIdentifier);
    }

    @Inject
    public AbstractSyncDataTreeChangeListener(DataBroker dataBroker, LogicalDatastoreType datastoreType,
                                              InstanceIdentifier<T> instanceIdentifier) {
        super(dataBroker, datastoreType, instanceIdentifier);
    }

    @Override
    public final void onDataTreeChanged(@Nonnull Collection<DataTreeModification<T>> collection) {
        DataTreeChangeListenerActions.super.onDataTreeChanged(collection);
    }
}
