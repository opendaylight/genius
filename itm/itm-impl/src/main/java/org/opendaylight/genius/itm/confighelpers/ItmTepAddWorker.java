/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmTepAddWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepAddWorker.class);

    private final DataBroker dataBroker;
    private final IdManagerService idManagerService;
    private List<DPNTEPsInfo> meshedDpnList;
    private final List<DPNTEPsInfo> cfgdDpnList ;
    private final IMdsalApiManager mdsalManager;
    private final List<HwVtep> cfgdHwVteps;
    private final ItmConfig itmConfig;

    public ItmTepAddWorker(List<DPNTEPsInfo> cfgdDpnList, List<HwVtep> hwVtepList, DataBroker broker,
                           IdManagerService idManagerService, IMdsalApiManager mdsalManager,
                           ItmConfig itmConfig) {
        this.transportZoneName = transportZoneName;
        this.cfgdDpnList = cfgdDpnList ;
        this.dataBroker = broker ;
        this.idManagerService = idManagerService;
        this.mdsalManager = mdsalManager;
        this.cfgdHwVteps = hwVtepList;
        this.itmConfig = itmConfig;
        LOG.trace("ItmTepAddWorker initialized with  DpnList {}",cfgdDpnList);
        LOG.trace("ItmTepAddWorker initialized with  hwvteplist {}",hwVtepList);
    }

    @Override
    public List<ListenableFuture<Void>> call() {
        List<ListenableFuture<Void>> futures = new ArrayList<>() ;
        this.meshedDpnList = ItmUtils.getTunnelMeshInfo(dataBroker) ;
        LOG.debug("Invoking Internal Tunnel build method with Configured DpnList {} ; Meshed DpnList {} ",
                cfgdDpnList, meshedDpnList);
        futures.addAll(ItmInternalTunnelAddWorker.build_all_tunnels(dataBroker, idManagerService, mdsalManager,
                cfgdDpnList, meshedDpnList, itmConfig)) ;
        // IF EXTERNAL TUNNELS NEEDS TO BE BUILT, DO IT HERE. IT COULD BE TO DC GATEWAY OR TOR SWITCH
        List<DcGatewayIp> dcGatewayIpList = ItmUtils.getDcGatewayIpList(dataBroker);
        if (dcGatewayIpList != null && !dcGatewayIpList.isEmpty()) {
            for (DcGatewayIp dcGatewayIp : dcGatewayIpList) {
                futures.addAll(ItmExternalTunnelAddWorker.buildTunnelsToExternalEndPoint(dataBroker, idManagerService,
                        cfgdDpnList, dcGatewayIp.getIpAddress(), dcGatewayIp.getTunnnelType(), itmConfig));
            }
        }
        //futures.addAll(ItmExternalTunnelAddWorker.buildTunnelsToExternalEndPoint(dataBroker,meshedDpnList,extIp);
        LOG.debug("invoking build hwVtepTunnels with hwVteplist {}", cfgdHwVteps);
        futures.addAll(ItmExternalTunnelAddWorker.buildHwVtepsTunnels(dataBroker, idManagerService,
                cfgdDpnList,cfgdHwVteps));
        return futures ;
    }

    @Override
    public String toString() {
        return "ItmTepAddWorker  { "
                + "Configured Dpn List : " + cfgdDpnList
                + "  Meshed Dpn List : " + meshedDpnList + " }" ;
    }
}
