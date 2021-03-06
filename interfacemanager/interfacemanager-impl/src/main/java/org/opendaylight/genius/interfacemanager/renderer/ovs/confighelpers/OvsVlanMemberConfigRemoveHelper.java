/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import static org.opendaylight.mdsal.binding.util.Datastore.CONFIGURATION;
import static org.opendaylight.mdsal.binding.util.Datastore.OPERATIONAL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.commons.InterfaceMetaUtils;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info._interface.parent.entry.InterfaceChildEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OvsVlanMemberConfigRemoveHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsVlanMemberConfigRemoveHelper.class);

    private final ManagedNewTransactionRunner txRunner;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;
    private final InterfaceMetaUtils interfaceMetaUtils;

    @Inject
    public OvsVlanMemberConfigRemoveHelper(@Reference DataBroker dataBroker,
            InterfaceManagerCommonUtils interfaceManagerCommonUtils, InterfaceMetaUtils interfaceMetaUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
        this.interfaceMetaUtils = interfaceMetaUtils;
    }

    public List<? extends ListenableFuture<?>> removeConfiguration(ParentRefs parentRefs, Interface interfaceOld) {
        LOG.debug("remove vlan member configuration {}", interfaceOld.getName());

        InterfaceParentEntryKey interfaceParentEntryKey = new InterfaceParentEntryKey(parentRefs.getParentInterface());
        InstanceIdentifier<InterfaceParentEntry> interfaceParentEntryIid = InterfaceMetaUtils
                .getInterfaceParentEntryIdentifier(interfaceParentEntryKey);
        InterfaceParentEntry interfaceParentEntry = interfaceMetaUtils
                .getInterfaceParentEntryFromConfigDS(interfaceParentEntryIid);

        if (interfaceParentEntry == null) {
            return Collections.emptyList();
        }

        return txRunner.applyWithNewTransactionChainAndClose(txChain -> {
            List<ListenableFuture<?>> futures = new ArrayList<>();

            // Configuration changes
            futures.add(txChain.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION, tx -> {
                // Delete the interface child information
                Map<InterfaceChildEntryKey, InterfaceChildEntry> interfaceChildEntries =
                        interfaceParentEntry.nonnullInterfaceChildEntry();
                InterfaceChildEntryKey interfaceChildEntryKey = new InterfaceChildEntryKey(interfaceOld.getName());
                InstanceIdentifier<InterfaceChildEntry> interfaceChildEntryIid = InterfaceMetaUtils
                    .getInterfaceChildEntryIdentifier(interfaceParentEntryKey, interfaceChildEntryKey);
                tx.delete(interfaceChildEntryIid);
                // If this is the last child, remove the interface parent info as well.
                if (interfaceChildEntries.size() <= 1) {
                    tx.delete(interfaceParentEntryIid);
                }
            }));

            // Operational changes
            futures.add(txChain.callWithNewReadWriteTransactionAndSubmit(OPERATIONAL, tx -> {
                org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                    .ietf.interfaces.rev140508.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                    .getInterfaceState(tx, parentRefs.getParentInterface());
                if (ifState != null) {
                    LOG.debug("delete vlan member interface state {}", interfaceOld.getName());
                    Uint64 dpId = IfmUtil.getDpnFromInterface(ifState);
                    interfaceManagerCommonUtils.deleteInterfaceStateInformation(interfaceOld.getName(), tx);
                    // TODO skitt The following is another configuration transaction, we'll deal with it later
                    FlowBasedServicesUtils.removeIngressFlow(interfaceOld.getName(), dpId, txRunner, futures);
                }
            }));

            return futures;
        });
    }
}
