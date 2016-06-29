/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ItmTepRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger logger = LoggerFactory.getLogger(ItmTepRemoveWorker.class ) ;
    private DataBroker dataBroker;
    private List<DPNTEPsInfo> delDpnList ;
    private List<DPNTEPsInfo> meshedDpnList ;
    private IdManagerService idManagerService;
    private IMdsalApiManager mdsalManager;
    private List<HwVtep> cfgdHwVteps;

    public ItmTepRemoveWorker(List<DPNTEPsInfo> delDpnList, List<HwVtep> delHwList, DataBroker broker, IdManagerService idManagerService, IMdsalApiManager mdsalManager) {
        this.delDpnList = delDpnList ;
        this.dataBroker = broker ;
        this.idManagerService = idManagerService;
        this.mdsalManager = mdsalManager;
        this.cfgdHwVteps = delHwList;
        logger.trace("ItmTepRemoveWorker initialized with  DpnList {}",delDpnList );
        logger.trace("ItmTepRemoveWorker initialized with  cfgdHwTeps {}",delHwList );
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>() ;
        this.meshedDpnList = ItmUtils.getTunnelMeshInfo(dataBroker) ;
        futures.addAll( ItmInternalTunnelDeleteWorker.deleteTunnels(dataBroker, idManagerService, mdsalManager, delDpnList, meshedDpnList));
        logger.debug("Invoking Internal Tunnel delete method with DpnList to be deleted {} ; Meshed DpnList {} ",delDpnList, meshedDpnList );
        // IF EXTERNAL TUNNELS NEEDS TO BE DELETED, DO IT HERE, IT COULD BE TO DC GATEWAY OR TOR SWITCH
        List<DcGatewayIp> dcGatewayIpList = ItmUtils.getDcGatewayIpList(dataBroker);
        if(dcGatewayIpList != null && !dcGatewayIpList.isEmpty()){
            List<DPNTEPsInfo>  dpnDeleteList = new ArrayList<>();
            for( DPNTEPsInfo dpnTEPInfo : delDpnList){
                List<TunnelEndPoints> tunnelEndPointsList = dpnTEPInfo.getTunnelEndPoints();
                if(tunnelEndPointsList.size() == 1){
                    dpnDeleteList.add(dpnTEPInfo);
                }
                else{
                    logger.error("DPNTEPInfo not available in data store for dpnId" + dpnTEPInfo.getDPNID() + ". Unable to delete external tunnel for dpn ");
                }
            }
            for(DcGatewayIp dcGatewayIp : dcGatewayIpList){
                futures.addAll(ItmExternalTunnelDeleteWorker.deleteTunnels(dataBroker, idManagerService,  dpnDeleteList, dcGatewayIp.getIpAddress(), dcGatewayIp.getTunnnelType()));
            }
        }

        futures.addAll(ItmExternalTunnelDeleteWorker.deleteHwVtepsTunnels(dataBroker, idManagerService,delDpnList,cfgdHwVteps));
        return futures ;
    }

    @Override
    public String toString() {
        return "ItmTepRemoveWorker  { " +
        "Delete Dpn List : " + delDpnList + " }" ;
    }
}
