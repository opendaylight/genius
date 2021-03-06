/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.renderer.ovs.confighelpers;

import static org.opendaylight.mdsal.binding.util.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OvsVlanMemberConfigAddHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsVlanMemberConfigAddHelper.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final ManagedNewTransactionRunner txRunner;
    private final InterfaceManagerCommonUtils interfaceManagerCommonUtils;

    @Inject
    public OvsVlanMemberConfigAddHelper(@Reference DataBroker dataBroker,
            InterfaceManagerCommonUtils interfaceManagerCommonUtils) {
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.interfaceManagerCommonUtils = interfaceManagerCommonUtils;
    }

    public List<? extends ListenableFuture<?>> addConfiguration(ParentRefs parentRefs, Interface interfaceNew) {
        LOG.info("adding vlan member configuration for interface {}", interfaceNew.getName());
        EVENT_LOGGER.debug("IFM-OVSVlanMemberConfig,ADD Parent {},Child {}", parentRefs.getParentInterface(),
                interfaceNew.getName());
        List<ListenableFuture<?>> futures = new ArrayList<>();
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
            tx -> interfaceManagerCommonUtils.createInterfaceChildEntry(tx, parentRefs.getParentInterface(),
                    interfaceNew.getName())));

        InterfaceKey interfaceKey = new InterfaceKey(parentRefs.getParentInterface());
        Interface ifaceParent = interfaceManagerCommonUtils.getInterfaceFromConfigDS(interfaceKey);
        if (ifaceParent == null) {
            LOG.info("Parent Interface: {} not found when adding child interface: {}", parentRefs.getParentInterface(),
                    interfaceNew.getName());
            return futures;
        }

        IfL2vlan parentIfL2Vlan = ifaceParent.augmentation(IfL2vlan.class);
        if (parentIfL2Vlan == null || parentIfL2Vlan.getL2vlanMode() != IfL2vlan.L2vlanMode.Trunk) {
            LOG.error("Parent Interface: {} not of trunk Type when adding trunk-member: {}", ifaceParent, interfaceNew);
            return futures;
        }

        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface ifState = interfaceManagerCommonUtils
                .getInterfaceState(parentRefs.getParentInterface());
        interfaceManagerCommonUtils.addStateEntry(interfaceNew.getName(), futures, ifState);
        return futures;
    }
}