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
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for Interface in Configuration DS.
 * This deletes the entries incase of upgrade from non-ItmDirectTunnel way to ItmDirectTunnel.
 */
@Singleton
public class InterfaceConfigListener extends AsyncDataTreeChangeListenerBase<Interface, InterfaceConfigListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceConfigListener.class);

    private final DataBroker dataBroker;
    private final JobCoordinator coordinator;

    @Inject
    public InterfaceConfigListener(final DataBroker dataBroker, final IInterfaceManager interfaceManager,
                                   final JobCoordinator coordinator) {
        super(Interface.class, InterfaceConfigListener.class);
        this.dataBroker = dataBroker;
        this.coordinator = coordinator;
        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            LOG.debug("Itm Scale Improvement is Enabled, hence registering IntefaceConfigListener");
            this.registerListener(LogicalDatastoreType.CONFIGURATION, this.dataBroker);
        } else {
            LOG.debug("Itm Scale Improvement is not Enabled, therefore not registering InterfaceConfigListener");
        }
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    }

    @Override
    protected InterfaceConfigListener getDataTreeChangeListener() {
        return InterfaceConfigListener.this;
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceOld) {
        LOG.debug("REMOVE hit in interfaceConfigListenerITM");
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceOld, Interface interfaceNew) {
        LOG.debug("UPDATE hit in interfaceConfigListenerITM");
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceNew) {
        IfTunnel ifTunnel = interfaceNew.getAugmentation(IfTunnel.class);
        if (ifTunnel != null) {
            if (ifTunnel.isInternal()) {
                LOG.debug("ADD Received Interface Add Event: {}, {}", key, interfaceNew);
                RendererConfigDeleteWorker configWorker = new RendererConfigDeleteWorker(key, interfaceNew);
                coordinator.enqueueJob(interfaceNew.getName(), configWorker, ITMConstants.JOB_MAX_RETRIES);
            }
        }
    }

    private static class RendererConfigDeleteWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceNew;

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