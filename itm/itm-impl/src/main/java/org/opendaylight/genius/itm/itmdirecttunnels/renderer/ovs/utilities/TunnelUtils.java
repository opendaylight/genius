/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.cache.DpnsTepsStateCache;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.TunnelEndPointInfo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* Equivalent of InterfaceManagerCommonUtils */
public final class TunnelUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TunnelUtils.class);

    private TunnelUtils() {
    }

    public static DpnTepInterfaceInfo getTunnelFromConfigDS(String tunnelName, DataBroker dataBroker,
                                                            DpnsTepsStateCache dpnTepStateCache,
                                                            TunnelEndPointInfo tunnelEndPointInfo) {
        DpnTepInterfaceInfo dpnTepInfo = null ;
        if (tunnelEndPointInfo != null) {
            dpnTepInfo = dpnTepStateCache.getDpnTepInterfaceInfo(tunnelEndPointInfo.getSrcEndPointInfo(),
                            tunnelEndPointInfo.getDstEndPointInfo());
            if (dpnTepInfo != null) {
                return dpnTepInfo;
            }
            else {
                //TODO read if from Datastore fill in IfTunnel
            }
        }
        return dpnTepInfo;
    }

    public static StateTunnelList getTunnelFromOperationalDS(String tunnelName, DataBroker dataBroker,
                                                             TunnelStateCache tunnelStateCache) {

        try {
            Optional<StateTunnelList> stateTunnelList = tunnelStateCache
                    .get(getStateTunnelListIdentifier(new StateTunnelListKey(tunnelName)));
            if (stateTunnelList.isPresent()) {
                return stateTunnelList.get();
            }
        } catch (ReadFailedException exception) {
            LOG.debug("read failed for tunnel {}, while reading TunnelStateCache", tunnelName);
        }
        InstanceIdentifier<StateTunnelList> stateTnlII =
                ItmUtils.buildStateTunnelListId(new StateTunnelListKey(tunnelName));
        Optional<StateTunnelList> tnlStateOptional =
                ItmUtils.read(LogicalDatastoreType.OPERATIONAL, stateTnlII, dataBroker);
        if (!tnlStateOptional.isPresent()) {
            return null;
        }
        return tnlStateOptional.get();
    }

    public static InstanceIdentifier<StateTunnelList> getStateTunnelListIdentifier(StateTunnelListKey key) {
        return InstanceIdentifier.builder(TunnelsState.class).child(StateTunnelList.class, key).build();
    }

    public static StateTunnelList addStateEntry(TunnelEndPointInfo tunnelEndPointInfo, String interfaceName,
                                                ManagedNewTransactionRunner transaction, IdManagerService idManager,
                                                OperStatus operStatus, Interface.AdminStatus adminStatus,
                                                NodeConnectorId nodeConnectorId,
                                                DPNTEPsInfoCache dpntePsInfoCache) {
        LOG.debug("Start addStateEntry adding interface state for {}", interfaceName);
        return new StateTunnelListBuilder().build();
    }
}
