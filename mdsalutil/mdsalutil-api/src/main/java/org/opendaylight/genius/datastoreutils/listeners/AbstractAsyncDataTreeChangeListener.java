/*
 * Copyright (c) 2017 Ericsson S.A. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.datastoreutils.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Abstract class providing some common functionality to specific listeners.
 * @deprecated Please use {@link org.opendaylight.genius.tools.mdsal.listener.AbstractAsyncDataTreeChangeListener}
 *     instead of this!
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_SUPERCLASS")
@Deprecated
public abstract class AbstractAsyncDataTreeChangeListener<T extends DataObject> extends
        org.opendaylight.genius.tools.mdsal.listener.AbstractAsyncDataTreeChangeListener<T> {

    @Inject
    public AbstractAsyncDataTreeChangeListener(DataBroker dataBroker, DataTreeIdentifier<T> dataTreeIdentifier,
                                               ExecutorService executorService) {
        super(dataBroker, dataTreeIdentifier, executorService);
    }

    @Inject
    public AbstractAsyncDataTreeChangeListener(DataBroker dataBroker, LogicalDatastoreType datastoreType,
                                               InstanceIdentifier<T> instanceIdentifier,
                                               ExecutorService executorService) {
        super(dataBroker, datastoreType, instanceIdentifier, executorService);
    }

    @Inject
    public AbstractAsyncDataTreeChangeListener(DataBroker dataBroker,
                                               LogicalDatastoreType datastoreType,
                                               InstanceIdentifier<T> instanceIdentifier,
                                               ExecutorService executorService,
                                               MetricProvider metricProvider) {
        super(dataBroker, datastoreType, instanceIdentifier, executorService, metricProvider);
    }
}
