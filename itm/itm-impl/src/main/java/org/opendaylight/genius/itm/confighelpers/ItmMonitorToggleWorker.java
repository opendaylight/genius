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
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ItmMonitorToggleWorker implements Callable<List<ListenableFuture<Void>>> {
    private static final Logger logger = LoggerFactory.getLogger(ItmMonitorToggleWorker.class) ;
    private DataBroker dataBroker;
    private String tzone;
    private boolean enabled;
    private Class<? extends TunnelMonitoringTypeBase> monitorProtocol = ITMConstants.DEFAULT_MONITOR_PROTOCOL;

    public  ItmMonitorToggleWorker(String tzone,boolean enabled, Class<? extends TunnelMonitoringTypeBase> monitorProtocol, DataBroker dataBroker){
        this.dataBroker = dataBroker;
        this.tzone = tzone;
        this.enabled = enabled;
        this.monitorProtocol = monitorProtocol;
        logger.trace("ItmMonitorToggleWorker initialized with  tzone {} and toggleBoolean {}",tzone,enabled );
        logger.debug("TunnelMonitorToggleWorker with monitor protocol = {} ",monitorProtocol);
    }

    @Override public List<ListenableFuture<Void>> call() throws Exception {
        List<ListenableFuture<Void>> futures = new ArrayList<>() ;
        logger.debug("ItmMonitorToggleWorker invoked with tzone = {} enabled {}",tzone,enabled );
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        toggleTunnelMonitoring(enabled,tzone,t);
        futures.add(t.submit());
        return futures;
    }

    private void toggleTunnelMonitoring(Boolean enabled, String tzone, WriteTransaction t) {
        List<String> TunnelList = ItmUtils.getInternalTunnelInterfaces(dataBroker);
        logger.debug("toggleTunnelMonitoring: TunnelList size {}", TunnelList.size());
        InstanceIdentifier<TunnelMonitorParams> iid = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
        TunnelMonitorParams protocolBuilder = new TunnelMonitorParamsBuilder().setEnabled(enabled).setMonitorProtocol(monitorProtocol).build();
        logger.debug("toggleTunnelMonitoring: Updating Operational DS");
        ItmUtils.asyncUpdate(LogicalDatastoreType.OPERATIONAL,iid, protocolBuilder, dataBroker, ItmUtils.DEFAULT_CALLBACK);
        if(TunnelList !=null &&!TunnelList.isEmpty()) {
            for (String tunnel : TunnelList) {
                toggle(tunnel, enabled, t);
            }
        }
    }

    private void toggle(String tunnelInterfaceName, boolean enabled, WriteTransaction t) {
        if(tunnelInterfaceName!=null) {
            InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(tunnelInterfaceName);
            logger.debug("TunnelMonitorToggleWorker: tunnelInterfaceName: {}, monitorProtocol = {},  monitorEnable = {} ",tunnelInterfaceName, monitorProtocol, enabled);
            IfTunnel tunnel = new IfTunnelBuilder().setMonitorEnabled(enabled).setMonitorProtocol(monitorProtocol).build();
            InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(tunnelInterfaceName))
                    .addAugmentation(IfTunnel.class, tunnel);
            t.merge(LogicalDatastoreType.CONFIGURATION, trunkIdentifier, builder.build());
       }
    }
}




