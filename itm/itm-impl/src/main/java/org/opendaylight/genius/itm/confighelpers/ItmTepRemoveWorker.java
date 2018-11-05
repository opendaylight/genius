/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import static org.opendaylight.genius.itm.impl.ItmUtils.nullToEmpty;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmTepRemoveWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepRemoveWorker.class);

    private final DataBroker dataBroker;
    private final List<DPNTEPsInfo> delDpnList ;
    private Collection<DPNTEPsInfo> meshedDpnList ;
    private final IMdsalApiManager mdsalManager;
    private final List<HwVtep> cfgdHwVteps;
    private final TransportZone originalTZone;
    private final ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker;
    private final DPNTEPsInfoCache dpnTEPsInfoCache;

    public ItmTepRemoveWorker(List<DPNTEPsInfo> delDpnList, List<HwVtep> delHwList, TransportZone originalTZone,
            DataBroker broker, IMdsalApiManager mdsalManager,
            ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker, DPNTEPsInfoCache dpnTEPsInfoCache) {
        this.delDpnList = delDpnList;
        this.dataBroker = broker;
        this.mdsalManager = mdsalManager;
        this.cfgdHwVteps = delHwList;
        this.originalTZone = originalTZone;
        this.itmInternalTunnelDeleteWorker = itmInternalTunnelDeleteWorker;
        this.dpnTEPsInfoCache = dpnTEPsInfoCache;
        LOG.trace("ItmTepRemoveWorker initialized with  DpnList {}", delDpnList);
        LOG.trace("ItmTepRemoveWorker initialized with  cfgdHwTeps {}", delHwList);
    }

    @Override
    public List<ListenableFuture<Void>> call() {
        List<ListenableFuture<Void>> futures = new ArrayList<>() ;
        this.meshedDpnList = dpnTEPsInfoCache.getAllPresent();
        futures.addAll(itmInternalTunnelDeleteWorker.deleteTunnels(mdsalManager, delDpnList,
                meshedDpnList));
        LOG.debug("Invoking Internal Tunnel delete method with DpnList to be deleted {} ; Meshed DpnList {} ",
                delDpnList, meshedDpnList);
        // IF EXTERNAL TUNNELS NEEDS TO BE DELETED, DO IT HERE, IT COULD BE TO DC GATEWAY OR TOR SWITCH
        List<DcGatewayIp> dcGatewayIpList = ItmUtils.getDcGatewayIpList(dataBroker);
        if (dcGatewayIpList != null && !dcGatewayIpList.isEmpty()) {
            List<DPNTEPsInfo>  dpnDeleteList = new ArrayList<>();
            for (DPNTEPsInfo dpnTEPInfo : delDpnList) {
                List<TunnelEndPoints> tunnelEndPointsList = nullToEmpty(dpnTEPInfo.getTunnelEndPoints());
                if (tunnelEndPointsList.size() == 1) {
                    dpnDeleteList.add(dpnTEPInfo);
                } else {
                    LOG.error("DPNTEPInfo not available in data store for dpnId {}. Unable to delete external tunnel "
                            + "for dpn ", dpnTEPInfo.getDPNID());
                }
            }
            for (DcGatewayIp dcGatewayIp : dcGatewayIpList) {
                futures.addAll(ItmExternalTunnelDeleteWorker.deleteTunnels(dataBroker,
                        dpnDeleteList , meshedDpnList, dcGatewayIp.getIpAddress(), dcGatewayIp.getTunnnelType()));
            }
        }

        futures.addAll(ItmExternalTunnelDeleteWorker.deleteHwVtepsTunnels(dataBroker, delDpnList,
                cfgdHwVteps, this.originalTZone));
        return futures ;
    }

    @Override
    public String toString() {
        return "ItmTepRemoveWorker  { "
                + "Delete Dpn List : " + delDpnList + " }" ;
    }
}
