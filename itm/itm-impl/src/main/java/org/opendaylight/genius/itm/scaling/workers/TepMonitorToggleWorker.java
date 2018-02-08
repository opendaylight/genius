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
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.scaling.renderer.ovs.confighelpers.OvsTunnelConfigUpdateHelper;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.Remote;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

public class TepMonitorToggleWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger logger = LoggerFactory.getLogger(TepMonitorToggleWorker.class) ;
    private DataBroker dataBroker;
    private String tzone;
    private boolean enabled;
    private Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;

    public TepMonitorToggleWorker(String tzone, boolean enabled, Class<? extends TunnelMonitoringTypeBase> monitorProtocol, DataBroker dataBroker){
        this.dataBroker = dataBroker;
        this.tzone = tzone;
        this.enabled = enabled;
        this.monitorProtocol = monitorProtocol;
        logger.trace("TepMonitorToggleWorker initialized with  tzone {} and toggleBoolean {}",tzone,enabled );
        logger.debug("TepMonitorToggleWorker with monitor protocol = {} ",monitorProtocol);
    }

    @Override public List<ListenableFuture<Void>> call() throws Exception {
        logger.debug("TepMonitorToggleWorker invoked with tzone = {} enabled {}",tzone,enabled );
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        toggleTunnelMonitoring(enabled,tzone,t);
        return Collections.singletonList(t.submit());
    }

    private void toggleTunnelMonitoring(Boolean enabled, String tzone, WriteTransaction t) {
        List<DpnsTeps> dpnsTepsList = ItmScaleUtils.getAllDpnsTeps(dataBroker);
        logger.debug("toggleTunnelMonitoring: DpnsTepsList size {}", dpnsTepsList.size());
        InstanceIdentifier<TunnelMonitorParams> iid = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
        TunnelMonitorParams protocolBuilder = new TunnelMonitorParamsBuilder().setEnabled(enabled).setMonitorProtocol(monitorProtocol).build();
        logger.debug("toggleTunnelMonitoring: Updating Operational DS");
        ItmUtils.asyncUpdate(LogicalDatastoreType.OPERATIONAL,iid, protocolBuilder, dataBroker, ItmUtils.DEFAULT_CALLBACK);
        if(dpnsTepsList !=null &&!dpnsTepsList.isEmpty()) {
            for (DpnsTeps dpnTeps : dpnsTepsList) {
                toggle(dpnTeps, enabled, t);
            }
        }
    }

    private void toggle(DpnsTeps dpnTeps, boolean enabled, WriteTransaction t) {
        if(dpnTeps!=null) {
            List<RemoteDpns> remoteDpnTepNewList = new ArrayList<>();
            for (RemoteDpns remoteDpn : dpnTeps.getRemoteDpns()) {
                logger.debug("TepMonitorToggleWorker: tunnelInterfaceName: {}, monitorProtocol = {},  monitorEnable = {} ",
                        remoteDpn.getTunnelName(), monitorProtocol, enabled);
                RemoteDpnsBuilder remoteDpnNewBld  = new RemoteDpnsBuilder(remoteDpn);
                RemoteDpns remoteDpnNew = remoteDpnNewBld.setMonitoringEnabled(enabled).build();
                remoteDpnTepNewList.add(remoteDpnNew);
                // Update the parameters on the switch
                OvsTunnelConfigUpdateHelper.updateConfiguration(dataBroker, dpnTeps.getSourceDpnId(), remoteDpnNew);
            }
            InstanceIdentifier<DpnsTeps> iid = InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class,
                    new DpnsTepsKey(dpnTeps.getSourceDpnId())).build();
            DpnsTepsBuilder builder = new DpnsTepsBuilder().setKey(new DpnsTepsKey(dpnTeps.getSourceDpnId())).
                    setRemoteDpns(remoteDpnTepNewList);
            t.merge(LogicalDatastoreType.CONFIGURATION, iid, builder.build());
       }
    }
}




