/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev150701.dpn.endpoints.DPNTEPsInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ItmTepAddWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger logger = LoggerFactory.getLogger(ItmTepAddWorker.class ) ;
    private DataBroker dataBroker;
    private IdManagerService idManagerService;
    private List<DPNTEPsInfo> meshedDpnList;
    private List<DPNTEPsInfo> cfgdDpnList ;
    private IMdsalApiManager mdsalManager;
    private List<HwVtep> cfgdHwVteps;

    public ItmTepAddWorker(List<DPNTEPsInfo> cfgdDpnList, List<HwVtep> hwVtepList, DataBroker broker, IdManagerService idManagerService, IMdsalApiManager mdsalManager) {
        this.cfgdDpnList = cfgdDpnList ;
        this.dataBroker = broker ;
        this.idManagerService = idManagerService;
        this.mdsalManager = mdsalManager;
        this.cfgdHwVteps = hwVtepList;
        logger.trace("ItmTepAddWorker initialized with  DpnList {}",cfgdDpnList );
        logger.trace("ItmTepAddWorker initialized with  hwvteplist {}",hwVtepList);
    }

    @Override
    public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>() ;
        this.meshedDpnList = ItmUtils.getTunnelMeshInfo(dataBroker) ;
        logger.debug("Invoking Internal Tunnel build method with Configured DpnList {} ; Meshed DpnList {} ",cfgdDpnList, meshedDpnList );
        futures.addAll( ItmInternalTunnelAddWorker.build_all_tunnels(dataBroker, idManagerService,mdsalManager, cfgdDpnList, meshedDpnList) ) ;
        // IF EXTERNAL TUNNELS NEEDS TO BE BUILT, DO IT HERE. IT COULD BE TO DC GATEWAY OR TOR SWITCH
        //futures.addAll(ItmExternalTunnelAddWorker.buildTunnelsToExternalEndPoint(dataBroker,meshedDpnList, extIp) ;
        logger.debug("invoking build hwVtepTunnels with hwVteplist {}", cfgdHwVteps );
        futures.addAll(ItmExternalTunnelAddWorker.buildHwVtepsTunnels(dataBroker, idManagerService,cfgdDpnList,cfgdHwVteps));
        return futures ;
    }

    @Override
    public String toString() {
        return "ItmTepAddWorker  { " +
        "Configured Dpn List : " + cfgdDpnList +
        "  Meshed Dpn List : " + meshedDpnList + " }" ;
    }
}
