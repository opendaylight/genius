/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.AlivenessMonitorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class OvsVlanMemberConfigUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsVlanMemberConfigUpdateHelper.class);
    public static List<ListenableFuture<Void>> updateConfiguration(DataBroker dataBroker, AlivenessMonitorService alivenessMonitorService, ParentRefs parentRefsNew,
                                                                   Interface interfaceOld, IfL2vlan ifL2vlanNew,
                                                                   Interface interfaceNew, IdManagerService idManager,
                                                                   IMdsalApiManager mdsalApiManager) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        ParentRefs parentRefsOld = interfaceOld.getAugmentation(ParentRefs.class);

        InterfaceParentEntryKey interfaceParentEntryKey =
                new InterfaceParentEntryKey(parentRefsOld.getParentInterface());
        InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(interfaceOld.getName());
        InterfaceChildEntry interfaceChildEntry =
                InterfaceMetaUtils.getInterfaceChildEntryFromConfigDS(interfaceParentEntryKey, interfaceChildEntryKey,
                        dataBroker);

        if (interfaceChildEntry == null) {
            futures.addAll(OvsInterfaceConfigAddHelper.addConfiguration(dataBroker,
                    interfaceNew.getAugmentation(ParentRefs.class), interfaceNew, idManager, alivenessMonitorService, mdsalApiManager));
            return futures;
        }

        IfL2vlan ifL2vlanOld = interfaceOld.getAugmentation(IfL2vlan.class);
        if (ifL2vlanOld == null || (ifL2vlanNew.getL2vlanMode() != ifL2vlanOld.getL2vlanMode())) {
            LOG.error("Configuration Error. Vlan Mode Change of Vlan Trunk Member {} as new trunk member: {} is " +
                    "not allowed.", interfaceOld, interfaceNew);
            return futures;
        }

        if (ifL2vlanOld.getVlanId() != ifL2vlanNew.getVlanId() ||
                !parentRefsOld.getParentInterface().equals(parentRefsNew.getParentInterface())) {
            futures.addAll(OvsVlanMemberConfigRemoveHelper.removeConfiguration(dataBroker, parentRefsOld, interfaceOld,
                    ifL2vlanOld, idManager));
            futures.addAll(OvsVlanMemberConfigAddHelper.addConfiguration(dataBroker, parentRefsNew, interfaceNew,
                    ifL2vlanNew, idManager));
            return futures;
        }

        if (interfaceNew.isEnabled() == interfaceOld.isEnabled()) {
            return futures;
        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface pifState =
                InterfaceManagerCommonUtils.getInterfaceStateFromOperDS(parentRefsNew.getParentInterface(), dataBroker);
        if (pifState != null) {
            OperStatus operStatus = OperStatus.Down;
            if (interfaceNew.isEnabled()) {
                operStatus = pifState.getOperStatus();
            }

            WriteTransaction t = dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId =
                    IfmUtil.buildStateInterfaceId(interfaceNew.getName());
            InterfaceBuilder ifaceBuilder = new InterfaceBuilder();
            ifaceBuilder.setOperStatus(operStatus);
            ifaceBuilder.setKey(IfmUtil.getStateInterfaceKeyFromName(interfaceNew.getName()));

            t.merge(LogicalDatastoreType.OPERATIONAL, ifStateId, ifaceBuilder.build());
            futures.add(t.submit());
        }

        return futures;
    }
}