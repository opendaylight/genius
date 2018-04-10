/*
 * Copyright (c) 2017, 2018 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract class providing some common functionality to specific listeners.
 * @deprecated Please use {@link
 * org.opendaylight.genius.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener} instead of this!
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
@Deprecated
public abstract class AbstractClusteredSyncDataTreeChangeListener<T extends DataObject> extends
        org.opendaylight.genius.tools.mdsal.listener.AbstractClusteredSyncDataTreeChangeListener<T> {

    public AbstractClusteredSyncDataTreeChangeListener(DataBroker dataBroker,
                                                       DataTreeIdentifier<T> dataTreeIdentifier) {
        super(dataBroker, dataTreeIdentifier);
    }

    public AbstractClusteredSyncDataTreeChangeListener(DataBroker dataBroker, LogicalDatastoreType datastoreType,
                                                       InstanceIdentifier<T> instanceIdentifier) {
        super(dataBroker, datastoreType, instanceIdentifier);
    }

    public AbstractClusteredSyncDataTreeChangeListener(DataBroker dataBroker,
                                                       LogicalDatastoreType datastoreType,
                                                       InstanceIdentifier<T> instanceIdentifier,
                                                       MetricProvider metricProvider) {
        super(dataBroker, datastoreType, instanceIdentifier, metricProvider);
    }
}
