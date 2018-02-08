/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.workers;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.confighelpers.OvsTunnelConfigUpdateHelper;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class TepMonitorIntervalWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger logger = LoggerFactory.getLogger(TepMonitorIntervalWorker.class) ;
    private DataBroker dataBroker;
    private String tzone;
    private Integer interval;

    public TepMonitorIntervalWorker(String tzone, Integer interval, DataBroker dataBroker){
        this.dataBroker = dataBroker;
        this.tzone = tzone;
        this.interval = interval;
        logger.debug("TepMonitorIntervalWorker: monitorInterval = {}",interval);
        logger.trace("TepMonitorToggleWorker initialized with  tzone {} and Interval {}",tzone,interval );
    }

    @Override public List<ListenableFuture<Void>> call() throws Exception {
        logger.debug("Invoked TepMonitorIntervalWorker tzone = {} Interval= {}",tzone,interval );
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        toggleTunnelMonitoring(interval,tzone,t);
        return Collections.singletonList(t.submit());
    }

    private void toggleTunnelMonitoring(Integer interval, String tzone, WriteTransaction t) {
        List<DpnsTeps> dpnsTepsList = ItmScaleUtils.getAllDpnsTeps(dataBroker);
        logger.debug("toggleTunnelMonitoring: DpnsTepsList size {}", dpnsTepsList.size());
        InstanceIdentifier<TunnelMonitorInterval> iid = InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
        TunnelMonitorInterval intervalBuilder = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
        ItmUtils.asyncUpdate(LogicalDatastoreType.OPERATIONAL,iid, intervalBuilder, dataBroker, ItmUtils.DEFAULT_CALLBACK);
        if(dpnsTepsList !=null &&!dpnsTepsList.isEmpty()) {
            for (DpnsTeps dpnTeps : dpnsTepsList) {
                toggle(dpnTeps, interval, t);
            }
        }
    }

    private void toggle(DpnsTeps dpnTeps, Integer interval, WriteTransaction t) {
        if(dpnTeps!=null) {
            List<RemoteDpns> remoteDpnTepList = dpnTeps.getRemoteDpns();
            for (RemoteDpns remoteDpn : remoteDpnTepList) {
                logger.debug("TepMonitorToggleWorker: tunnelInterfaceName: {}, monitorEnable = {} ",
                        remoteDpn.getTunnelName(), interval);
                // Do this if monitor interval is added per tunnel
                /*
                RemoteDpns remoteDpnNew = new RemoteDpnsBuilder().setKey(
                        new RemoteDpnsKey(remoteDpn.getDestinationDpnId())).setbuild();
                        */
                // Update the parameters on the switch
               // OvsTunnelConfigUpdateHelper.updateConfiguration(dataBroker, dpnTeps.getSourceDpnId(), remoteDpnNew);
                OvsTunnelConfigUpdateHelper.updateConfiguration(dataBroker, dpnTeps.getSourceDpnId(), remoteDpn);
            }
            // Do this if monitor interval is added per tunnel
            /*
            InstanceIdentifier<DpnsTeps> iid = InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class,
                    new DpnsTepsKey(dpnTeps.getSourceDpnId())).build();
            DpnsTepsBuilder builder = new DpnsTepsBuilder().setKey(new DpnsTepsKey(dpnTeps.getSourceDpnId())).
                    setRemoteDpns(remoteDpnTepList);
            t.merge(LogicalDatastoreType.CONFIGURATION, iid, builder.build());
            */
        }
    }
}


