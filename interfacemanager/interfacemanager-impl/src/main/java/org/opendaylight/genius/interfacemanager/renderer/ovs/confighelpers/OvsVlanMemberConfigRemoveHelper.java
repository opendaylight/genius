/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsVlanMemberConfigRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsVlanMemberConfigRemoveHelper.class);

    private final DataBroker dataBroker;
    private final IdManagerService idManager;

    public OvsVlanMemberConfigRemoveHelper(DataBroker dataBroker, IdManagerService idManager) {
        this.dataBroker = dataBroker;
        this.idManager = idManager;
    }

    public List<ListenableFuture<Void>> removeConfiguration(ParentRefs parentRefs, Interface interfaceOld) {
        LOG.debug("remove vlan member configuration {}", interfaceOld.getName());
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction defaultConfigShardTransaction = dataBroker.newWriteOnlyTransaction();
        WriteTransaction defaultOperShardTransaction = dataBroker.newWriteOnlyTransaction();

        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentRefs.getParentInterface());
        InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIid = InterfaceMetaUtils
                .getInterfaceParentEntryIdentifier(interfaceParentEntryKey);
        InterfaceParentEntry interfaceParentEntry = InterfaceMetaUtils
                .getInterfaceParentEntryFromConfigDS(interfaceParentEntryIid, dataBroker);

        if (interfaceParentEntry == null) {
            return futures;
        }

        // Delete the interface child information
        List<InterfaceChildEntry> interfaceChildEntries = interfaceParentEntry.getInterfaceChildEntry();
        InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(interfaceOld.getName());
        InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryIid = InterfaceMetaUtils
                .getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);
        defaultConfigShardTransaction.delete(LogicalDatastoreType.CONFIGURATION, interfaceChildEntryIid);
        // If this is the last child, remove the interface parent info as well.
        if (interfaceChildEntries.size() <= 1) {
            defaultConfigShardTransaction.delete(LogicalDatastoreType.CONFIGURATION, interfaceParentEntryIid);
        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
            .ietf.interfaces.rev140508.interfaces.state.Interface ifState = InterfaceManagerCommonUtils
                .getInterfaceState(parentRefs.getParentInterface(), dataBroker);
        if (ifState != null) {
            LOG.debug("delete vlan member interface state {}", interfaceOld.getName());
            BigInteger dpId = IfmUtil.getDpnFromInterface(ifState);
            InterfaceManagerCommonUtils.deleteInterfaceStateInformation(interfaceOld.getName(),
                defaultOperShardTransaction,idManager);
            FlowBasedServicesUtils.removeIngressFlow(interfaceOld.getName(), dpId, dataBroker, futures);
        }

        futures.add(defaultConfigShardTransaction.submit());
        futures.add(defaultOperShardTransaction.submit());
        return futures;
    }
}
