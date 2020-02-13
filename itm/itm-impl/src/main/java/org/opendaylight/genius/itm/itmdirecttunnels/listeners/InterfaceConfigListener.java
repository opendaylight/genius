/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.itmdirecttunnels.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for Interface in Configuration DS.
 * This deletes the entries incase of upgrade from non-ItmDirectTunnel way to ItmDirectTunnel.
 */

public class InterfaceConfigListener extends AbstractSyncDataTreeChangeListener<Interface> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceConfigListener.class);

    private final DataBroker dataBroker;
    private final JobCoordinator coordinator;

    public InterfaceConfigListener(final DataBroker dataBroker, final JobCoordinator coordinator) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(Interface.class));
        this.dataBroker = dataBroker;
        this.coordinator = coordinator;
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<Interface> key, @NonNull Interface interfaceOld) {
        LOG.debug("REMOVE hit in interfaceConfigListenerITM");
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Interface> key, @NonNull Interface interfaceOld,
                       @NonNull Interface interfaceNew) {
        LOG.debug("UPDATE hit in interfaceConfigListenerITM");
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Interface> key, @NonNull Interface interfaceNew) {
        IfTunnel ifTunnel = interfaceNew.augmentation(IfTunnel.class);
        if (ifTunnel != null) {
            if (ifTunnel.isInternal()) {
                LOG.debug("ADD Received Interface Add Event: {}, {}", key, interfaceNew);
                RendererConfigDeleteWorker configWorker = new RendererConfigDeleteWorker(key, interfaceNew);
                coordinator.enqueueJob(interfaceNew.getName(), configWorker, ITMConstants.JOB_MAX_RETRIES);
            }
        }
    }

    private static class RendererConfigDeleteWorker implements Callable<List<ListenableFuture<Void>>> {
        final InstanceIdentifier<Interface> key;
        final Interface interfaceNew;

        RendererConfigDeleteWorker(InstanceIdentifier<Interface> key, Interface interfaceNew) {
            this.key = key;
            this.interfaceNew = interfaceNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            ITMBatchingUtils.delete(key, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
            return Collections.emptyList();
        }

        @Override
        public String toString() {
            return "RendererConfigDeleteWorker{key=" + key + ", interfaceNew=" + interfaceNew + '\'' + '}';
        }
    }
}
