/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.servicebinding.BindServiceUtils;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.OfTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.of.teps.state.OfTepKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OfTepStateCache extends DataObjectCache<String, OfTep> {
    private static final Logger LOG = LoggerFactory.getLogger(OfTepStateCache.class);

    private final ManagedNewTransactionRunner txRunner;
    private final IInterfaceManager interfaceManager;
    private final DirectTunnelUtils directTunnelUtils;
    private final List<ListenableFuture<?>> futures = new ArrayList<>();

    @Inject
    public OfTepStateCache(DataBroker dataBroker, CacheProvider cacheProvider,
                           IInterfaceManager interfaceManager, DirectTunnelUtils directTunnelUtils) {
        super(OfTep.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
            InstanceIdentifier.builder(OfTepsState.class).child(OfTep.class).build(), cacheProvider,
            (iid, ofTepList) -> ofTepList.getOfPortName(), ofPortName -> InstanceIdentifier.builder(OfTepsState.class)
                    .child(OfTep.class, new OfTepKey(ofPortName)).build());
        this.interfaceManager = interfaceManager;
        this.directTunnelUtils = directTunnelUtils;
        this.txRunner =  new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    @Override
    protected void added(InstanceIdentifier<OfTep> path, OfTep ofTep) {
        LOG.debug("Adding interface name to internal cache.");
        List<String> childLowerLayerIfList = new ArrayList<>();
        String lowref = MDSALUtil.NODE_PREFIX + MDSALUtil.SEPARATOR + ofTep.getSourceDpnId()
                + MDSALUtil.SEPARATOR + ofTep.getPortNumber();
        childLowerLayerIfList.add(0, lowref);
        InterfaceBuilder ifaceBuilder = new InterfaceBuilder().setIfIndex(ofTep.getIfIndex().intValue())
                .withKey(new InterfaceKey(ofTep.getOfPortName()))
                .setLowerLayerIf(childLowerLayerIfList).setType(Tunnel.class)
                .setName(ofTep.getOfPortName());
        interfaceManager.addInternalTunnelToCache(ofTep.getOfPortName(), ifaceBuilder.build());

        if (directTunnelUtils.isEntityOwner()) {
            LOG.info("Binding default egress dispatcher service for{}", ofTep.getOfPortName());
            BindServiceUtils.bindDefaultEgressDispatcherService(txRunner, futures, "VXLAN_TRUNK_INTERFACE",
                    String.valueOf(ofTep.getPortNumber()), ofTep.getOfPortName(),
                    ofTep.getIfIndex());
        }
    }

    @Override
    protected void removed(InstanceIdentifier<OfTep> path, OfTep ofTep) {
        BindServiceUtils.unbindService(futures, txRunner, ofTep.getOfPortName());
        interfaceManager.removeInternalTunnelFromCache(ofTep.getOfPortName());
    }
}
