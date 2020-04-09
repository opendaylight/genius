/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.recovery.impl.InterfaceServiceRecoveryHandler;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsInterfaceConfigAddHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsInterfaceConfigRemoveHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers.OvsInterfaceConfigUpdateHelper;
import org.opendaylight.genius.interfacemanager.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.utils.clustering.EntityOwnershipUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.serviceutils.srm.RecoverableListener;
import org.opendaylight.serviceutils.srm.ServiceRecoveryRegistry;
import org.opendaylight.serviceutils.tools.listener.AbstractClusteredSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
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
        extends AbstractClusteredSyncDataTreeChangeListener<Interface>
        implements RecoverableListener {

    private static final Logger LOG = LoggerFactory.getLogger(InterfaceConfigListener.class);

    private final InterfacemgrProvider interfaceMgrProvider;
    private final EntityOwnershipUtils entityOwnershipUtils;
    private final JobCoordinator coordinator;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final OvsInterfaceConfigRemoveHelper ovsInterfaceConfigRemoveHelper;
    private final OvsInterfaceConfigAddHelper ovsInterfaceConfigAddHelper;
    private final OvsInterfaceConfigUpdateHelper ovsInterfaceConfigUpdateHelper;

    @Inject
    public InterfaceConfigListener(@Reference DataBroker dataBroker,
                                   InterfacemgrProvider interfaceMgrProvider,
                                   EntityOwnershipUtils entityOwnershipUtils,
                                   @Reference JobCoordinator coordinator,
                                   InterfaceManagerCommonUtils interfaceManagerCommonUtils,
                                   OvsInterfaceConfigRemoveHelper ovsInterfaceConfigRemoveHelper,
                                   OvsInterfaceConfigAddHelper ovsInterfaceConfigAddHelper,
                                   OvsInterfaceConfigUpdateHelper ovsInterfaceConfigUpdateHelper,
                                   InterfaceServiceRecoveryHandler interfaceServiceRecoveryHandler,
                                   @Reference ServiceRecoveryRegistry serviceRecoveryRegistry) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(Interfaces.class).child(Interface.class));
        this.interfaceMgrProvider = interfaceMgrProvider;
        this.entityOwnershipUtils = entityOwnershipUtils;
        this.coordinator = coordinator;
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.ovsInterfaceConfigRemoveHelper = ovsInterfaceConfigRemoveHelper;
        this.ovsInterfaceConfigAddHelper = ovsInterfaceConfigAddHelper;
        this.ovsInterfaceConfigUpdateHelper = ovsInterfaceConfigUpdateHelper;
        serviceRecoveryRegistry.addRecoverableListener(interfaceServiceRecoveryHandler.buildServiceRegistryKey(),
                this);
    }

    @Override
    public void registerListener() {
        super.register();
    }

    @Override
    public void deregisterListener() {
        close();
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<Interface> instanceIdentifier, @NonNull Interface removedInterface) {
        interfaceManagerCommonUtils.removeFromInterfaceCache(removedInterface);

        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received Interface Remove Event: {}, {}", instanceIdentifier, removedInterface);
        ParentRefs parentRefs = removedInterface.augmentation(ParentRefs.class);
        if (parentRefs == null
                || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
            LOG.debug("parent refs not specified for {}", removedInterface.getName());
            return;
        }
        String synchronizationKey = InterfaceManagerCommonUtils.isTunnelInterface(removedInterface)
            ? removedInterface.getName() : parentRefs.getParentInterface();
        coordinator.enqueueJob(synchronizationKey,
            () -> ovsInterfaceConfigRemoveHelper.removeConfiguration(removedInterface, parentRefs),
            IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Interface> instanceIdentifier, @NonNull Interface originalInterface,
                       @NonNull Interface updatedInterface) {
        interfaceManagerCommonUtils.addInterfaceToCache(updatedInterface);

        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received Interface Update Event: {}, {}, {}", instanceIdentifier, originalInterface,
                  updatedInterface);
        ParentRefs parentRefs = updatedInterface.augmentation(ParentRefs.class);
        if (parentRefs == null || parentRefs.getParentInterface() == null) {
            // If parentRefs are missing, try to find a matching parent and
            // update - this will trigger another DCN
            updateInterfaceParentRefs(updatedInterface);
        }

        if (parentRefs == null
                || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
            LOG.debug("parent refs not specified for {}, or parentRefs {} missing DPN/parentInterface",
                    updatedInterface.getName(), parentRefs);
            return;
        }
        String synchronizationKey = getSynchronizationKey(updatedInterface, parentRefs);
        coordinator.enqueueJob(synchronizationKey, () -> ovsInterfaceConfigUpdateHelper
                .updateConfiguration(updatedInterface, originalInterface), IfmConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Interface> instanceIdentifier, @NonNull Interface newInterface) {
        interfaceManagerCommonUtils.addInterfaceToCache(newInterface);

        if (!entityOwnershipUtils.isEntityOwner(IfmConstants.INTERFACE_CONFIG_ENTITY,
                IfmConstants.INTERFACE_CONFIG_ENTITY)) {
            return;
        }
        LOG.debug("Received Interface Add Event: {}, {}", instanceIdentifier, newInterface);
        ParentRefs parentRefs = newInterface.augmentation(ParentRefs.class);
        if (parentRefs == null || parentRefs.getParentInterface() == null) {
            // If parentRefs are missing, try to find a matching parent and
            // update - this will trigger another DCN
            updateInterfaceParentRefs(newInterface);
        }

        if (parentRefs == null
                || parentRefs.getDatapathNodeIdentifier() == null && parentRefs.getParentInterface() == null) {
            LOG.debug("parent refs not specified for {}", newInterface.getName());
            return;
        }
        String synchronizationKey = getSynchronizationKey(newInterface, parentRefs);
        coordinator.enqueueJob(synchronizationKey,
            () -> ovsInterfaceConfigAddHelper.addConfiguration(parentRefs, newInterface),
            IfmConstants.JOB_MAX_RETRIES);
    }

    private void updateInterfaceParentRefs(Interface iface) {
        if (InterfaceManagerCommonUtils.isTunnelInterface(iface)) {
            return; // update of parent refs is needed only for vm ports, and not tunnels
        }
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
    }

    private String getSynchronizationKey(Interface theInterface, ParentRefs theParentRefs) {
        if (InterfaceManagerCommonUtils.isOfTunnelInterface(theInterface)) {
            return SouthboundUtils.generateOfTunnelName(
                    theParentRefs.getDatapathNodeIdentifier(),
                    theInterface.augmentation(IfTunnel.class));
        } else if (InterfaceManagerCommonUtils.isTunnelInterface(theInterface)) {
            return theInterface.getName();
        } else {
            return theParentRefs.getParentInterface();
        }
    }
}
