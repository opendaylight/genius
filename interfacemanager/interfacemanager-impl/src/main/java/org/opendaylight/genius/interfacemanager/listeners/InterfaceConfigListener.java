/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsInterfaceConfigAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsInterfaceConfigRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsInterfaceConfigUpdateHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.IfmClusterUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for interface creation/removal/update in Configuration DS.
 * This is used to handle interfaces for base of-ports.
 */
public class InterfaceConfigListener extends AsyncClusteredDataTreeChangeListenerBase<Interface, InterfaceConfigListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceConfigListener.class);
    private DataBroker dataBroker;
    private IdManagerService idManager;
    private AlivenessMonitorService alivenessMonitorService;
    private IMdsalApiManager mdsalApiManager;

    public InterfaceConfigListener(final DataBroker dataBroker, final IdManagerService idManager,
                                   final AlivenessMonitorService alivenessMonitorService,
                                   final IMdsalApiManager mdsalApiManager) {
        super(Interface.class, InterfaceConfigListener.class);
        this.dataBroker = dataBroker;
        this.idManager = idManager;
        this.alivenessMonitorService = alivenessMonitorService;
        this.mdsalApiManager = mdsalApiManager;
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
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            LOG.debug("Received Interface Remove Event: {}, {}", key, interfaceOld);
            String ifName = interfaceOld.getName();
            ParentRefs parentRefs = interfaceOld.getAugmentation(ParentRefs.class);
            if (parentRefs == null || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
                LOG.warn("parent refs not specified for {}", interfaceOld.getName());
                return;
            }
            boolean isTunnelInterface = InterfaceManagerCommonUtils.isTunnelInterface(interfaceOld);
            parentRefs = updateParentInterface(isTunnelInterface, parentRefs);
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            RendererConfigRemoveWorker configWorker = new RendererConfigRemoveWorker(key, interfaceOld, ifName, parentRefs);
            String synchronizationKey = isTunnelInterface ?
                    parentRefs.getDatapathNodeIdentifier().toString() : parentRefs.getParentInterface();
            coordinator.enqueueJob(synchronizationKey, configWorker, IfmConstants.JOB_MAX_RETRIES);
        });
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceOld, Interface interfaceNew) {
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            LOG.debug("Received Interface Update Event: {}, {}, {}", key, interfaceOld, interfaceNew);
            String ifNameNew = interfaceNew.getName();
            ParentRefs parentRefs = interfaceNew.getAugmentation(ParentRefs.class);
            if (parentRefs == null || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
                LOG.warn("parent refs not specified for {}", interfaceNew.getName());
                return;
            }
            boolean isTunnelInterface = InterfaceManagerCommonUtils.isTunnelInterface(interfaceOld);
            parentRefs = updateParentInterface(isTunnelInterface, parentRefs);
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            RendererConfigUpdateWorker worker = new RendererConfigUpdateWorker(key, interfaceOld, interfaceNew, ifNameNew);
            String synchronizationKey = isTunnelInterface ?
                    interfaceOld.getName() : parentRefs.getParentInterface();
            coordinator.enqueueJob(synchronizationKey, worker, IfmConstants.JOB_MAX_RETRIES);
        });
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceNew) {
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            LOG.debug("Received Interface Add Event: {}, {}", key, interfaceNew);
            String ifName = interfaceNew.getName();
            ParentRefs parentRefs = interfaceNew.getAugmentation(ParentRefs.class);
            if (parentRefs == null || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
                LOG.warn("parent refs not specified for {}", interfaceNew.getName());
                return;
            }
            boolean isTunnelInterface = InterfaceManagerCommonUtils.isTunnelInterface(interfaceNew);
            parentRefs = updateParentInterface(isTunnelInterface, parentRefs);
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            RendererConfigAddWorker configWorker = new RendererConfigAddWorker(key, interfaceNew, parentRefs, ifName);
            String synchronizationKey = isTunnelInterface ?
                    interfaceNew.getName() : parentRefs.getParentInterface();
            coordinator.enqueueJob(synchronizationKey, configWorker, IfmConstants.JOB_MAX_RETRIES);
        });
    }

    private static ParentRefs updateParentInterface(boolean isTunnelInterface, ParentRefs parentRefs) {
        if (!isTunnelInterface && parentRefs.getDatapathNodeIdentifier() != null) {
            String parentInterface = parentRefs.getDatapathNodeIdentifier().toString() + IfmConstants.OF_URI_SEPARATOR +
                    parentRefs.getParentInterface();
            parentRefs = new ParentRefsBuilder(parentRefs).setParentInterface(parentInterface).build();
        }
        return parentRefs;
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceNew;
        String portName;
        ParentRefs parentRefs;

        public RendererConfigAddWorker(InstanceIdentifier<Interface> key, Interface interfaceNew,
                                       ParentRefs parentRefs, String portName) {
            this.key = key;
            this.interfaceNew = interfaceNew;
            this.portName = portName;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsInterfaceConfigAddHelper.addConfiguration(dataBroker, parentRefs, interfaceNew,
                    idManager, alivenessMonitorService, mdsalApiManager);
        }

        @Override
        public String toString() {
            return "RendererConfigAddWorker{" +
                    "key=" + key +
                    ", interfaceNew=" + interfaceNew +
                    ", portName='" + portName + '\'' +
                    '}';
        }
    }

    /**
     *
     */
    private class RendererConfigUpdateWorker implements Callable {
        InstanceIdentifier<Interface> key;
        Interface interfaceOld;
        Interface interfaceNew;
        String portNameNew;

        public RendererConfigUpdateWorker(InstanceIdentifier<Interface> key, Interface interfaceOld,
                                          Interface interfaceNew, String portNameNew) {
            this.key = key;
            this.interfaceOld = interfaceOld;
            this.interfaceNew = interfaceNew;
            this.portNameNew = portNameNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsInterfaceConfigUpdateHelper.updateConfiguration(dataBroker, alivenessMonitorService, idManager,
                    mdsalApiManager, interfaceNew, interfaceOld);
        }

        @Override
        public String toString() {
            return "RendererConfigUpdateWorker{" +
                    "key=" + key +
                    ", interfaceOld=" + interfaceOld +
                    ", interfaceNew=" + interfaceNew +
                    ", portNameNew='" + portNameNew + '\'' +
                    '}';
        }
    }

    /**
     *
     */
    private class RendererConfigRemoveWorker implements Callable {
        InstanceIdentifier<Interface> key;
        Interface interfaceOld;
        String portName;
        ParentRefs parentRefs;

        public RendererConfigRemoveWorker(InstanceIdentifier<Interface> key, Interface interfaceOld, String portName,
                                          ParentRefs parentRefs) {
            this.key = key;
            this.interfaceOld = interfaceOld;
            this.portName = portName;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsInterfaceConfigRemoveHelper.removeConfiguration(dataBroker, alivenessMonitorService,
                    interfaceOld, idManager, mdsalApiManager, parentRefs);
        }

        @Override
        public String toString() {
            return "RendererConfigRemoveWorker{" +
                    "key=" + key +
                    ", interfaceOld=" + interfaceOld +
                    ", portName='" + portName + '\'' +
                    '}';
        }
    }
}