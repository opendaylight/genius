/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.workers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TepInterfaceAddWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger LOG = LoggerFactory.getLogger(TepInterfaceAddWorker.class);

    private final DataBroker dataBroker;
    private final IdManagerService idManagerService;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final List<DPNTEPsInfo> cfgdDpnList ;
    private final IMdsalApiManager mdsalManager;
    private final List<HwVtep> cfgdHwVteps;
    private final ItmConfig itmConfig;
    private final TunnelMonitoringConfig tunnelMonitoringConfig;
    private final ItmExternalTunnelAddWorker itmExternalTunnelAddWorker;

    public TepInterfaceAddWorker(final List<DPNTEPsInfo> cfgdDpnList, final List<HwVtep> hwVtepList,
                                 final DataBroker broker, final IdManagerService idManagerService,
                                 final IMdsalApiManager mdsalManager, final ItmConfig itmConfig,
                                 final DPNTEPsInfoCache dpntePsInfoCache,
                                 final TunnelMonitoringConfig tunnelMonitoringConfig) {
        this.cfgdDpnList = cfgdDpnList ;
        this.dataBroker = broker ;
        this.idManagerService = idManagerService;
        this.mdsalManager = mdsalManager;
        this.cfgdHwVteps = hwVtepList;
        this.itmConfig = itmConfig;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.tunnelMonitoringConfig = tunnelMonitoringConfig;
        this.itmExternalTunnelAddWorker = new ItmExternalTunnelAddWorker(dataBroker, itmConfig, dpntePsInfoCache);
    }

    @Override
    public List<ListenableFuture<Void>> call() {
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        Collection<DPNTEPsInfo> meshedDpnList = dpntePsInfoCache.getAllPresent();
        LOG.debug("Invoking Tunnel Creation with Configured DpnList {} ; Meshed DpnList {} ", cfgdDpnList,
                meshedDpnList);
        // Wire the internal tunnels
        futures.addAll(TepInternalTunnelAddWorker.buildAllTunnels(dataBroker, mdsalManager, idManagerService,
                cfgdDpnList, meshedDpnList, tunnelMonitoringConfig));
        // Wire the external tunnel towards DC Gateway
        // IF EXTERNAL TUNNELS NEEDS TO BE BUILT, DO IT HERE. IT COULD BE TO DC GATEWAY OR TOR SWITCH
        List<DcGatewayIp> dcGatewayIpList = ItmUtils.getDcGatewayIpList(dataBroker);
        if (dcGatewayIpList != null && !dcGatewayIpList.isEmpty()) {
            for (DcGatewayIp dcGatewayIp : dcGatewayIpList) {
                futures.addAll(itmExternalTunnelAddWorker.buildTunnelsToExternalEndPoint(cfgdDpnList,
                        dcGatewayIp.getIpAddress(), dcGatewayIp.getTunnnelType()));
            }
        }
        // Wire the external tunnel to and from HW VTEP
        LOG.debug("invoking build hwVtepTunnels with hwVteplist {}", cfgdHwVteps);
        futures.addAll(itmExternalTunnelAddWorker.buildHwVtepsTunnels(cfgdDpnList, cfgdHwVteps));
        return futures ;
    }
}
