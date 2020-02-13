/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.DcGatewayIpList;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmTepAddWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmTepAddWorker.class);

    private final DataBroker dataBroker;
    private Collection<DPNTEPsInfo> meshedDpnList;
    private final List<DPNTEPsInfo> cfgdDpnList ;
    private final IMdsalApiManager mdsalManager;
    private final List<HwVtep> cfgdHwVteps;
    private final ItmInternalTunnelAddWorker itmInternalTunnelAddWorker;
    private final ItmExternalTunnelAddWorker externalTunnelAddWorker;
    private final ManagedNewTransactionRunner txRunner;

    public ItmTepAddWorker(List<DPNTEPsInfo> cfgdDpnList, List<HwVtep> hwVtepList, DataBroker broker,
                           IMdsalApiManager mdsalManager, ItmInternalTunnelAddWorker itmInternalTunnelAddWorker,
                           ItmExternalTunnelAddWorker externalTunnelAddWorker) {
        this.cfgdDpnList = cfgdDpnList ;
        this.dataBroker = broker ;
        this.mdsalManager = mdsalManager;
        this.cfgdHwVteps = hwVtepList;
        this.itmInternalTunnelAddWorker = itmInternalTunnelAddWorker;
        this.externalTunnelAddWorker = externalTunnelAddWorker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        LOG.trace("ItmTepAddWorker initialized with  DpnList {}",cfgdDpnList);
        LOG.trace("ItmTepAddWorker initialized with  hwvteplist {}",hwVtepList);
    }

    @Override
    public List<ListenableFuture<Void>> call() {
        List<ListenableFuture<Void>> futures = new ArrayList<>() ;
        this.meshedDpnList = ItmUtils.getDpnTEPsInfos(dataBroker);
        LOG.debug("Invoking Internal Tunnel build method with Configured DpnList {} ; Meshed DpnList {} ",
                cfgdDpnList, meshedDpnList);
        futures.addAll(itmInternalTunnelAddWorker.buildAllTunnels(mdsalManager, cfgdDpnList, meshedDpnList));

        // IF EXTERNAL TUNNELS NEEDS TO BE BUILT, DO IT HERE. IT COULD BE TO DC GATEWAY OR TOR SWITCH
        futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(CONFIGURATION,
            tx -> {
                Optional<DcGatewayIpList> optional = tx.read(InstanceIdentifier.builder(DcGatewayIpList.class)
                        .build()).get();
                if (optional.isPresent()) {
                    optional.get().getDcGatewayIp().forEach(dcGatewayIp ->
                        externalTunnelAddWorker.buildTunnelsToExternalEndPoint(cfgdDpnList, dcGatewayIp.getIpAddress(),
                        dcGatewayIp.getTunnnelType(), tx));
                }
            }
        ));
        //futures.addAll(ItmExternalTunnelAddWorker.buildTunnelsToExternalEndPoint(dataBroker,meshedDpnList, extIp) ;
        LOG.debug("invoking build hwVtepTunnels with hwVteplist {}", cfgdHwVteps);
        futures.add(txRunner.callWithNewReadWriteTransactionAndSubmit(Datastore.CONFIGURATION,
            tx -> externalTunnelAddWorker.buildHwVtepsTunnels(cfgdDpnList, cfgdHwVteps, tx)));
        return futures;
    }

    @Override
    public String toString() {
        return "ItmTepAddWorker  { "
                + "Configured Dpn List : " + cfgdDpnList
                + "  Meshed Dpn List : " + meshedDpnList + " }" ;
    }
}
