/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for interface creation/removal/update in Configuration DS.
 * This is used to handle interfaces for base of-ports.
 */
@Singleton
public class InterfaceConfigListener
        extends AsyncClusteredDataTreeChangeListenerBase<Interface, InterfaceConfigListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceConfigListener.class);
    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final IdManagerService idManager;
    private final AlivenessMonitorService alivenessMonitorService;
    private final IMdsalApiManager mdsalApiManager;
    private final InterfacemgrProvider interfaceMgrProvider;

    @Inject
    public InterfaceConfigListener(final DataBroker dataBroker, final IdManagerService idManager,
            final IMdsalApiManager mdsalApiManager, final InterfacemgrProvider interfaceMgrProvider,
            final AlivenessMonitorService alivenessMonitorService) {
        super(Interface.class, InterfaceConfigListener.class);
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.idManager = idManager;
        this.mdsalApiManager = mdsalApiManager;
        this.interfaceMgrProvider = interfaceMgrProvider;
        this.alivenessMonitorService = alivenessMonitorService;
        this.registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    }

    @Override
    protected InterfaceConfigListener getDataTreeChangeListener() {
        return InterfaceConfigListener.this;
    }

    private void updateInterfaceParentRefs(Interface iface) {
        String ifName = iface.getName();
        // try to acquire the parent interface name from Southbound
        String parentRefName = interfaceMgrProvider.getParentRefNameForInterface(ifName);
        if (parentRefName == null) {
            LOG.debug("parent refs not specified for {}, failed acquiring it from southbound", ifName);
            return;
        }
        LOG.debug("retrieved parent ref {} for interface {} from southbound, updating parentRef in datastore",
                parentRefName, ifName);
        interfaceMgrProvider.updateInterfaceParentRef(ifName, parentRefName, false);
        return;
    }

    @Override
    protected void remove(InstanceIdentifier<Interface> key, Interface interfaceOld) {
        InterfaceManagerCommonUtils.removeFromInterfaceCache(interfaceOld);
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            LOG.debug("Received Interface Remove Event: {}, {}", key, interfaceOld);
            String ifName = interfaceOld.getName();
            ParentRefs parentRefs = interfaceOld.getAugmentation(ParentRefs.class);
            if (parentRefs == null
                    || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
                LOG.debug("parent refs not specified for {}", interfaceOld.getName());
                return;
            }
            boolean isTunnelInterface = InterfaceManagerCommonUtils.isTunnelInterface(interfaceOld);
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            RendererConfigRemoveWorker configWorker = new RendererConfigRemoveWorker(key, interfaceOld,
                    interfaceOld.getName(), parentRefs);
            String synchronizationKey = isTunnelInterface ? parentRefs.getDatapathNodeIdentifier().toString()
                    : parentRefs.getParentInterface();
            coordinator.enqueueJob(synchronizationKey, configWorker, IfmConstants.JOB_MAX_RETRIES);
        }, IfmClusterUtils.INTERFACE_CONFIG_ENTITY);
    }

    @Override
    protected void update(InstanceIdentifier<Interface> key, Interface interfaceOld, Interface interfaceNew) {
        InterfaceManagerCommonUtils.addInterfaceToCache(interfaceNew);
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            LOG.debug("Received Interface Update Event: {}, {}, {}", key, interfaceOld, interfaceNew);
            ParentRefs parentRefs = interfaceNew.getAugmentation(ParentRefs.class);
            if (parentRefs == null || parentRefs.getParentInterface() == null
                    && !InterfaceManagerCommonUtils.isTunnelInterface(interfaceNew)) {
                // If parentRefs are missing, try to find a matching parent and
                // update - this will trigger another DCN
                updateInterfaceParentRefs(interfaceNew);
            }

            String ifNameNew = interfaceNew.getName();
            if (parentRefs == null
                    || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
                LOG.debug("parent refs not specified for {}, or parentRefs {} missing DPN/parentInterface",
                        interfaceNew.getName(), parentRefs);
                return;
            }
            boolean isTunnelInterface = InterfaceManagerCommonUtils.isTunnelInterface(interfaceOld);
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            RendererConfigUpdateWorker configWorker = new RendererConfigUpdateWorker(key, interfaceOld, interfaceNew,
                    interfaceNew.getName());
            String synchronizationKey = isTunnelInterface ? interfaceOld.getName() : parentRefs.getParentInterface();
            coordinator.enqueueJob(synchronizationKey, configWorker, IfmConstants.JOB_MAX_RETRIES);

        }, IfmClusterUtils.INTERFACE_CONFIG_ENTITY);
    }

    @Override
    protected void add(InstanceIdentifier<Interface> key, Interface interfaceNew) {
        InterfaceManagerCommonUtils.addInterfaceToCache(interfaceNew);
        IfmClusterUtils.runOnlyInLeaderNode(() -> {
            LOG.debug("Received Interface Add Event: {}, {}", key, interfaceNew);
            ParentRefs parentRefs = interfaceNew.getAugmentation(ParentRefs.class);
            if (parentRefs == null || parentRefs.getParentInterface() == null) {
                // If parentRefs are missing, try to find a matching parent and
                // update - this will trigger another DCN
                updateInterfaceParentRefs(interfaceNew);
            }

            String ifName = interfaceNew.getName();
            if (parentRefs == null
                    || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
                LOG.debug("parent refs not specified for {}", interfaceNew.getName());
                return;
            }
            boolean isTunnelInterface = InterfaceManagerCommonUtils.isTunnelInterface(interfaceNew);
            DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
            RendererConfigAddWorker configWorker = new RendererConfigAddWorker(key, interfaceNew, parentRefs,
                    interfaceNew.getName());
            String synchronizationKey = isTunnelInterface ? interfaceNew.getName() : parentRefs.getParentInterface();
            coordinator.enqueueJob(synchronizationKey, configWorker, IfmConstants.JOB_MAX_RETRIES);
        }, IfmClusterUtils.INTERFACE_CONFIG_ENTITY);
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceNew;
        String portName;
        ParentRefs parentRefs;

        RendererConfigAddWorker(InstanceIdentifier<Interface> key, Interface interfaceNew, ParentRefs parentRefs,
                String portName) {
            this.key = key;
            this.interfaceNew = interfaceNew;
            this.portName = portName;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // If another renderer(for eg: another OpenFlow based switch) needs to be supported, check
            // can be performed here to call the respective helpers.
            return OvsInterfaceConfigAddHelper.addConfiguration(dataBroker, txRunner, parentRefs, interfaceNew,
                    idManager, alivenessMonitorService, mdsalApiManager);
        }

        @Override
        public String toString() {
            return "RendererConfigAddWorker{" + "key=" + key + ", interfaceNew=" + interfaceNew + ", portName='"
                    + portName + '\'' + '}';
        }
    }

    private class RendererConfigUpdateWorker implements Callable {
        InstanceIdentifier<Interface> key;
        Interface interfaceOld;
        Interface interfaceNew;
        String portNameNew;

        RendererConfigUpdateWorker(InstanceIdentifier<Interface> key, Interface interfaceOld,
                Interface interfaceNew, String portNameNew) {
            this.key = key;
            this.interfaceOld = interfaceOld;
            this.interfaceNew = interfaceNew;
            this.portNameNew = portNameNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // If another renderer(for eg: another OpenFlow based switch) needs to be supported, check
            // can be performed here to call the respective helpers.
            return OvsInterfaceConfigUpdateHelper.updateConfiguration(dataBroker, txRunner, alivenessMonitorService,
                    idManager, mdsalApiManager, interfaceNew, interfaceOld);
        }

        @Override
        public String toString() {
            return "RendererConfigUpdateWorker{" + "key=" + key + ", interfaceOld=" + interfaceOld + ", interfaceNew="
                    + interfaceNew + ", portNameNew='" + portNameNew + '\'' + '}';
        }
    }

    private class RendererConfigRemoveWorker implements Callable {
        InstanceIdentifier<Interface> key;
        Interface interfaceOld;
        String portName;
        ParentRefs parentRefs;

        RendererConfigRemoveWorker(InstanceIdentifier<Interface> key, Interface interfaceOld, String portName,
                ParentRefs parentRefs) {
            this.key = key;
            this.interfaceOld = interfaceOld;
            this.portName = portName;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // If another renderer(for eg: HWVTEP) needs to be supported, check
            // can be performed here to call the respective helpers.
            return OvsInterfaceConfigRemoveHelper.removeConfiguration(dataBroker, txRunner, alivenessMonitorService,
                    interfaceOld, idManager, mdsalApiManager, parentRefs);
        }

        @Override
        public String toString() {
            return "RendererConfigRemoveWorker{" + "key=" + key + ", interfaceOld=" + interfaceOld + ", portName='"
                    + portName + '\'' + '}';
        }
    }
}
