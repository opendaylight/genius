/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import static org.opendaylight.genius.itm.impl.ItmUtils.nullToEmpty;
import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.TypedReadWriteTransaction;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.DcGatewayIpList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmTepRemoveWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepRemoveWorker.class);

    private final List<DPNTEPsInfo> delDpnList ;
    private final IMdsalApiManager mdsalManager;
    private final List<HwVtep> cfgdHwVteps;
    private final TransportZone originalTZone;
    private final ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker;
    private final DPNTEPsInfoCache dpnTEPsInfoCache;
    private final ManagedNewTransactionRunner txRunner;

    private Collection<DPNTEPsInfo> meshedDpnList ;

    public ItmTepRemoveWorker(List<DPNTEPsInfo> delDpnList, List<HwVtep> delHwList, TransportZone originalTZone,
                              IMdsalApiManager mdsalManager,
                              ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker,
                              DPNTEPsInfoCache dpnTEPsInfoCache, ManagedNewTransactionRunner txRunner) {
        this.delDpnList = delDpnList;
        this.mdsalManager = mdsalManager;
        this.cfgdHwVteps = delHwList;
        this.originalTZone = originalTZone;
        this.itmInternalTunnelDeleteWorker = itmInternalTunnelDeleteWorker;
        this.dpnTEPsInfoCache = dpnTEPsInfoCache;
        this.txRunner = txRunner;
        LOG.trace("ItmTepRemoveWorker initialized with  DpnList {}", delDpnList);
        LOG.trace("ItmTepRemoveWorker initialized with  cfgdHwTeps {}", delHwList);
    }

    @Override
    public List<ListenableFuture<Void>> call() {
        List<ListenableFuture<Void>> futures = new ArrayList<>() ;
        this.meshedDpnList = dpnTEPsInfoCache.getAllPresent();
        futures.addAll(itmInternalTunnelDeleteWorker.deleteTunnels(mdsalManager, delDpnList, meshedDpnList));
        LOG.debug("Invoking Internal Tunnel delete method with DpnList to be deleted {} ; Meshed DpnList {} ",
                delDpnList, meshedDpnList);
        // IF EXTERNAL TUNNELS NEEDS TO BE DELETED, DO IT HERE, IT COULD BE TO DC GATEWAY OR TOR SWITCH
        futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION,
            tx -> {
                Optional<DcGatewayIpList> optional = tx.read(InstanceIdentifier.builder(DcGatewayIpList.class)
                        .build()).get();
                if (optional.isPresent()) {
                    List<DcGatewayIp> dcGatewayIpList = optional.get().getDcGatewayIp();
                    if (dcGatewayIpList != null && !dcGatewayIpList.isEmpty()) {
                        processExternalTunnelTepDelete(dcGatewayIpList, tx);
                    }
                }
            }
        ));
        futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION,
            tx -> ItmExternalTunnelDeleteWorker.deleteHwVtepsTunnels(delDpnList, cfgdHwVteps, this.originalTZone, tx)));
        return futures;
    }

    @Override
    public String toString() {
        return "ItmTepRemoveWorker  { Delete Dpn List : " + delDpnList + " }" ;
    }

    private void processExternalTunnelTepDelete(Collection<DcGatewayIp> dcGatewayIpList,
                                                TypedReadWriteTransaction<Datastore.Configuration> tx) {
        List<DPNTEPsInfo>  dpnDeleteList = new ArrayList<>();
        for (DPNTEPsInfo dpnTEPInfo : delDpnList) {
            List<TunnelEndPoints> tunnelEndPointsList = dpnTEPInfo.getTunnelEndPoints();
            if (tunnelEndPointsList.size() == 1) {
                dpnDeleteList.add(dpnTEPInfo);
            } else {
                LOG.error("DPNTEPInfo not available in data store for dpnId {}. Unable to delete external tunnel "
                        + "for dpn ", dpnTEPInfo.getDPNID());
            }
        }
        dcGatewayIpList.forEach(dcGatewayIp -> ItmExternalTunnelDeleteWorker.deleteTunnels(dpnDeleteList,
            meshedDpnList, dcGatewayIp.getIpAddress(), dcGatewayIp.getTunnnelType(), tx));
    }
}
