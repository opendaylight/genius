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
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.recovery.impl.InterfaceServiceRecoveryHandler;
import org.opendaylight.genius.interfacemanager.recovery.listeners.RecoverableListener;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsInterfaceConfigAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsInterfaceConfigRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsInterfaceConfigUpdateHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
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
        extends AsyncClusteredDataTreeChangeListenerBase<Interface, InterfaceConfigListener>
        implements RecoverableListener {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceConfigListener.class);

    private final InterfacemgrProvider interfaceMgrProvider;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final DataBroker dataBroker;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final OvsInterfaceConfigRemoveHelper ovsInterfaceConfigRemoveHelper;
    private final OvsInterfaceConfigAddHelper ovsInterfaceConfigAddHelper;
    private final OvsInterfaceConfigUpdateHelper ovsInterfaceConfigUpdateHelper;

    @Inject
    public InterfaceConfigListener(final DataBroker dataBroker, final IdManagerService idManager,
            final IMdsalApiManager mdsalApiManager, final InterfacemgrProvider interfaceMgrProvider,
            final AlivenessMonitorService alivenessMonitorService, final EntityOwnershipUtils entityOwnershipUtils,
            final JobCoordinator coordinator, final InterfaceManagerCommonUtils interfaceManagerCommonUtils,
            final OvsInterfaceConfigRemoveHelper ovsInterfaceConfigRemoveHelper,
            final OvsInterfaceConfigAddHelper ovsInterfaceConfigAddHelper,
            final OvsInterfaceConfigUpdateHelper ovsInterfaceConfigUpdateHelper,
            final InterfaceServiceRecoveryHandler interfaceServiceRecoveryHandler) {
        super(Interface.class, InterfaceConfigListener.class);
        this.interfaceMgrProvider = interfaceMgrProvider;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.ovsInterfaceConfigRemoveHelper = ovsInterfaceConfigRemoveHelper;
        this.ovsInterfaceConfigAddHelper = ovsInterfaceConfigAddHelper;
        this.ovsInterfaceConfigUpdateHelper = ovsInterfaceConfigUpdateHelper;
        this.dataBroker = dataBroker;
        registerListener();
        interfaceServiceRecoveryHandler.addRecoverableListener(this);
    }

    @Override
    public void registerListener() {
        this.registerListener(LogicalDatastoreType.CONFIGURATION, dataBroker);
    }

    @Override
    public  void deregisterListener() {
        close();
    }

    @Override
    protected InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(Interfaces.class).child(Interface.class);
    }

    @Override
    protected InterfaceConfigListener getDataTreeChangeListener() {
        return InterfaceConfigListener.this;
    }

    private void updateInterfaceParentRefs(final Interface iface) {
        final String ifName = iface.getName();
        // try to acquire the parent interface name from Southbound
        final String parentRefName = interfaceMgrProvider.getParentRefNameForInterface(ifName);
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
    protected void remove(final InstanceIdentifier<Interface> key, final Interface interfaceOld) {
        interfaceManagerCommonUtils.removeFromInterfaceCache(interfaceOld);

        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received Interface Remove Event: {}, {}", key, interfaceOld);
        final ParentRefs parentRefs = interfaceOld.getAugmentation(ParentRefs.class);
        if (parentRefs == null
                || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
            LOG.debug("parent refs not specified for {}", interfaceOld.getName());
            return;
        }
        final boolean isTunnelInterface = InterfaceManagerCommonUtils.isTunnelInterface(interfaceOld);
        final RendererConfigRemoveWorker configWorker = new RendererConfigRemoveWorker(key, interfaceOld,
                interfaceOld.getName(), parentRefs);
        final String synchronizationKey = isTunnelInterface ? parentRefs.getDatapathNodeIdentifier().toString()
                : parentRefs.getParentInterface();
        coordinator.enqueueJob(synchronizationKey, configWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void update(final InstanceIdentifier<Interface> key, final Interface interfaceOld,
                          final Interface interfaceNew) {
        interfaceManagerCommonUtils.addInterfaceToCache(interfaceNew);

        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received Interface Update Event: {}, {}, {}", key, interfaceOld, interfaceNew);
        final ParentRefs parentRefs = interfaceNew.getAugmentation(ParentRefs.class);
        if (parentRefs == null || parentRefs.getParentInterface() == null
                && !InterfaceManagerCommonUtils.isTunnelInterface(interfaceNew)) {
            // If parentRefs are missing, try to find a matching parent and
            // update - this will trigger another DCN
            updateInterfaceParentRefs(interfaceNew);
        }

        if (parentRefs == null
                || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
            LOG.debug("parent refs not specified for {}, or parentRefs {} missing DPN/parentInterface",
                    interfaceNew.getName(), parentRefs);
            return;
        }
        final RendererConfigUpdateWorker configWorker = new RendererConfigUpdateWorker(key, interfaceOld, interfaceNew,
                interfaceNew.getName());
        final String synchronizationKey = getSynchronizationKey(interfaceNew, parentRefs);
        coordinator.enqueueJob(synchronizationKey, configWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    protected void add(final InstanceIdentifier<Interface> key, final Interface interfaceNew) {
        interfaceManagerCommonUtils.addInterfaceToCache(interfaceNew);

        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received Interface Add Event: {}, {}", key, interfaceNew);
        final ParentRefs parentRefs = interfaceNew.getAugmentation(ParentRefs.class);
        if (parentRefs == null || parentRefs.getParentInterface() == null) {
            // If parentRefs are missing, try to find a matching parent and
            // update - this will trigger another DCN
            updateInterfaceParentRefs(interfaceNew);
        }

        if (parentRefs == null
                || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
            LOG.debug("parent refs not specified for {}", interfaceNew.getName());
            return;
        }

        final RendererConfigAddWorker configWorker = new RendererConfigAddWorker(key, interfaceNew, parentRefs,
                interfaceNew.getName());
        final String synchronizationKey = getSynchronizationKey(interfaceNew, parentRefs);
        coordinator.enqueueJob(synchronizationKey, configWorker, IfmConstants.JOB_MAX_RETRIES);
    }

    private String getSynchronizationKey(final Interface theInterface, final ParentRefs theParentRefs) {
        if (InterfaceManagerCommonUtils.isOfTunnelInterface(theInterface)) {
            return SouthboundUtils.generateOfTunnelName(
                    theParentRefs.getDatapathNodeIdentifier(),
                    theInterface.getAugmentation(IfTunnel.class));
        } else if (InterfaceManagerCommonUtils.isTunnelInterface(theInterface)) {
            return theInterface.getName();
        } else {
            return theParentRefs.getParentInterface();
        }
    }

    private class RendererConfigAddWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<Interface> key;
        Interface interfaceNew;
        String portName;
        ParentRefs parentRefs;

        RendererConfigAddWorker(final InstanceIdentifier<Interface> key, final Interface interfaceNew,
                                final ParentRefs parentRefs,
                                final String portName) {
            this.key = key;
            this.interfaceNew = interfaceNew;
            this.portName = portName;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // If another renderer(for eg: another OpenFlow based switch) needs to be supported, check
            // can be performed here to call the respective helpers.
            return ovsInterfaceConfigAddHelper.addConfiguration(parentRefs, interfaceNew);
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

        RendererConfigUpdateWorker(final InstanceIdentifier<Interface> key, final Interface interfaceOld,
                                   final Interface interfaceNew, final String portNameNew) {
            this.key = key;
            this.interfaceOld = interfaceOld;
            this.interfaceNew = interfaceNew;
            this.portNameNew = portNameNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // If another renderer(for eg: another OpenFlow based switch) needs to be supported, check
            // can be performed here to call the respective helpers.
            return ovsInterfaceConfigUpdateHelper.updateConfiguration(interfaceNew, interfaceOld);
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

        RendererConfigRemoveWorker(final InstanceIdentifier<Interface> key, final Interface interfaceOld,
                                   final String portName,
                                   final ParentRefs parentRefs) {
            this.key = key;
            this.interfaceOld = interfaceOld;
            this.portName = portName;
            this.parentRefs = parentRefs;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            // If another renderer(for eg: HWVTEP) needs to be supported, check
            // can be performed here to call the respective helpers.
            return ovsInterfaceConfigRemoveHelper.removeConfiguration(interfaceOld, parentRefs);
        }

        @Override
        public String toString() {
            return "RendererConfigRemoveWorker{" + "key=" + key + ", interfaceOld=" + interfaceOld + ", portName='"
                    + portName + '\'' + '}';
        }
    }
}
