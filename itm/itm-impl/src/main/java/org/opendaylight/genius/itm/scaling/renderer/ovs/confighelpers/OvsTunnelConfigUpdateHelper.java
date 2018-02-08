/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.cache.TunnelStateCache;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.SouthboundUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelMetaUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.TunnelUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnels_state.StateTunnelList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsTunnelConfigUpdateHelper {
    private static final Logger LOG = LoggerFactory.getLogger(OvsTunnelConfigUpdateHelper.class);

    public static List<ListenableFuture<Void>> updateConfiguration(DataBroker dataBroker,
                                                                   BigInteger srcDpnId,
                                                                   RemoteDpns remoteDpn) {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        handleTunnelMonitorUpdates(futures, transaction, srcDpnId, remoteDpn, dataBroker);
        return futures;
/*
        // If any of the port attributes are modified, treat it as a delete and recreate scenario
        if(portAttributesModified(interfaceOld, interfaceNew)) {
            LOG.debug("port attributes modified, requires a delete and recreate of {} configuration", interfaceNew
                .getName());
            futures.addAll(OvsTunnelConfigRemoveHelper.removeConfiguration(dataBroker, interfaceOld, idManager,
                    mdsalApiManager, interfaceOld.getAugmentation(ParentRefs.class)));
            futures.addAll(OvsTunnelConfigAddHelper.addTunnelConfiguration(dataBroker, interfaceNew));
            return futures;
        }

        // If there is no operational state entry for the interface, treat it as create
        StateTunnelList stateTnlList = TunnelUtils.getTunnelFromOperationalDS(interfaceNew.getName(), dataBroker);
        if (stateTnlList == null) {
            futures.addAll(OvsTunnelConfigAddHelper.addTunnelConfiguration(dataBroker, interfaceNew));
            return futures;
        }

        //WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        if(TunnelMonitoringAttributesModified(interfaceOld, interfaceNew)){
            handleTunnelMonitorUpdates(futures, transaction, interfaceNew, interfaceOld, dataBroker);
            return futures;
        }
*/
        // ITM Direct Tunnes Is this Reqired ??
        /*
        if (interfaceNew.isEnabled() != interfaceOld.isEnabled()) {
            handleInterfaceAdminStateUpdates(futures, transaction, interfaceNew, dataBroker, ifState);
        }

        futures.add(transaction.submit());
        return futures;
        */
    }

    private static void handleTunnelMonitorUpdates(List<ListenableFuture<Void>> futures, WriteTransaction transaction,
                                                   BigInteger srcDpnId, RemoteDpns remoteDpn, DataBroker dataBroker){
        LOG.debug("tunnel monitoring attributes modified for interface {}", remoteDpn.getTunnelName());
        // update termination point on switch, if switch is connected
        OvsBridgeRefEntry bridgeRefEntry =
            TunnelMetaUtils.getBridgeReferenceForInterface(srcDpnId, dataBroker);
        // When DpnsTepsState has individual monitoring related params do this check then
      /* if(SouthboundUtils.isMonitorProtocolBfd(ifTunnel) && TunnelMetaUtils.bridgeExists(bridgeRefEntry, dataBroker)) {
            SouthboundUtils.updateBfdParamtersForTerminationPoint(bridgeRefEntry.getOvsBridgeReference().getValue(),
                    interfaceNew.getAugmentation(IfTunnel.class),
                    interfaceNew.getName(), transaction);
                    */
        if(TunnelMetaUtils.bridgeExists(bridgeRefEntry, dataBroker)){
            SouthboundUtils.updateBfdParamtersForTerminationPoint(bridgeRefEntry.getOvsBridgeReference().getValue(),
                dataBroker, remoteDpn, transaction);
        }
        futures.add(transaction.submit());
    }
}

