/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import static org.opendaylight.genius.infra.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OvsVlanMemberConfigUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsVlanMemberConfigUpdateHelper.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final ManagedNewTransactionRunner txRunner;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final OvsVlanMemberConfigAddHelper ovsVlanMemberConfigAddHelper;
    private final OvsVlanMemberConfigRemoveHelper ovsVlanMemberConfigRemoveHelper;
    private final InterfaceMetaUtils interfaceMetaUtils;

    @Inject
    public OvsVlanMemberConfigUpdateHelper(@Reference DataBroker dataBroker,
            InterfaceManagerCommonUtils interfaceManagerCommonUtils,
            OvsVlanMemberConfigAddHelper ovsVlanMemberConfigAddHelper,
            OvsVlanMemberConfigRemoveHelper ovsVlanMemberConfigRemoveHelper,
            InterfaceMetaUtils interfaceMetaUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.ovsVlanMemberConfigAddHelper = ovsVlanMemberConfigAddHelper;
        this.ovsVlanMemberConfigRemoveHelper = ovsVlanMemberConfigRemoveHelper;
        this.interfaceMetaUtils = interfaceMetaUtils;
    }

    public List<ListenableFuture<Void>> updateConfiguration(ParentRefs parentRefsNew, Interface interfaceOld,
            IfL2vlan ifL2vlanNew, Interface interfaceNew) {
        LOG.info("updating interface configuration for vlan memeber {} with parent-interface {}", interfaceNew
            .getName(), parentRefsNew.getParentInterface());
        EVENT_LOGGER.debug("IFM-OVSVlanMemberConfig,UPDATE Parent {},Child {}", parentRefsNew.getParentInterface(),
                interfaceNew.getName());
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        ParentRefs parentRefsOld = interfaceOld.augmentation(ParentRefs.class);

        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(
                parentRefsOld.getParentInterface());
        InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(interfaceOld.getName());
        InterfaceChildEntry interfaceChildEntry = interfaceMetaUtils
                .getInterfaceChildEntryFromConfigDS(interfaceParentEntryKey, interfaceChildEntryKey);

        if (interfaceChildEntry == null) {
            futures.addAll(ovsVlanMemberConfigAddHelper.addConfiguration(interfaceNew.augmentation(ParentRefs.class),
                    interfaceNew));
            return futures;
        }

        IfL2vlan ifL2vlanOld = interfaceOld.augmentation(IfL2vlan.class);
        if (ifL2vlanOld == null || ifL2vlanNew.getL2vlanMode() != ifL2vlanOld.getL2vlanMode()) {
            LOG.error("Configuration Error. Vlan Mode Change of Vlan Trunk Member {} as new trunk member: {} is "
                    + "not allowed.", interfaceOld, interfaceNew);
            return futures;
        }

        if (vlanIdModified(ifL2vlanOld.getVlanId(), ifL2vlanNew.getVlanId())
                || !Objects.equals(parentRefsOld.getParentInterface(), parentRefsNew.getParentInterface())) {
            LOG.info("vlan-id modified for interface {}", interfaceNew.getName());
            futures.addAll(ovsVlanMemberConfigRemoveHelper.removeConfiguration(parentRefsOld, interfaceOld));
            futures.addAll(ovsVlanMemberConfigAddHelper.addConfiguration(parentRefsNew, interfaceNew));
            return futures;
        }

        if (Objects.equals(interfaceNew.isEnabled(), interfaceOld.isEnabled())) {
            return futures;
        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .interfaces.rev140508.interfaces.state.Interface pifState = interfaceManagerCommonUtils.getInterfaceState(
                    parentRefsNew.getParentInterface());
        if (pifState != null) {
            OperStatus operStatus = interfaceNew.isEnabled() ? pifState.getOperStatus() : OperStatus.Down;
            LOG.info("admin-state modified for interface {}", interfaceNew.getName());
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(OPERATIONAL, tx -> {
                InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces
                        .rev140508.interfaces.state.Interface>
                        ifStateId = IfmUtil.buildStateInterfaceId(interfaceNew.getName());
                InterfaceBuilder ifaceBuilder = new InterfaceBuilder();
                ifaceBuilder.setOperStatus(operStatus);
                ifaceBuilder.withKey(IfmUtil.getStateInterfaceKeyFromName(interfaceNew.getName()));

                tx.merge(ifStateId, ifaceBuilder.build());
            }));
        }

        return futures;
    }

    public static boolean vlanIdModified(VlanId vlanIdOld, VlanId vlanIdNew) {
        return !Objects.equals(vlanIdOld, vlanIdNew);
    }
}
