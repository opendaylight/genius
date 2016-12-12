/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.MonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.tunnel.end.points.TzMembership;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.TransportZones;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.DeviceVteps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.Vteps;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmExternalTunnelAddWorker {
    private static final Logger logger = LoggerFactory.getLogger(ItmExternalTunnelAddWorker.class);

    public static List<ListenableFuture<Void>> buildTunnelsToExternalEndPoint(DataBroker dataBroker, IdManagerService idManagerService,
                                                                              List<DPNTEPsInfo> cfgDpnList, IpAddress extIp, Class<? extends TunnelTypeBase> tunType) {
         TunnelParameter tunnelParameter = new TunnelParameter.Builder().setIdManagerService(idManagerService).setCfgdDpnList(cfgDpnList).
                 setDataBroker(dataBroker).setMonitorEnabled(ItmUtils.readMonitoringStateFromCache(dataBroker)).setMonitorInterval(ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL).setMonitorProtocol(ITMConstants.DEFAULT_MONITOR_PROTOCOL).setTunnelType(tunType).setDestinationIP(extIp).build();
         TunnelWorkerInterface externalTunnelToDCGW = new ItmExternalTunnelToDCGw(tunnelParameter);
         return externalTunnelToDCGW.buildTunnelFutureList();
//        return null;
    }

    public static List<ListenableFuture<Void>> buildTunnelsFromDpnToExternalEndPoint(DataBroker dataBroker, IdManagerService idManagerService,
                                   List<BigInteger> dpnId, IpAddress extIp, Class<? extends TunnelTypeBase> tunType,Integer inMonitorInterval) {
        List<DPNTEPsInfo> cfgDpnList = dpnId == null ? ItmUtils.getTunnelMeshInfo(dataBroker) : ItmUtils.getDPNTEPListFromDPNId(dataBroker, dpnId);
        Integer monitorInterval = (inMonitorInterval == null)? ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL:inMonitorInterval;

        TunnelParameter tunnelParameter =  new TunnelParameter.Builder().setIdManagerService(idManagerService).setCfgdDpnList(cfgDpnList).
                setDataBroker(dataBroker).setMonitorEnabled(ItmUtils.readMonitoringStateFromCache(dataBroker)).setMonitorInterval(monitorInterval).setMonitorProtocol(ITMConstants.DEFAULT_MONITOR_PROTOCOL).setTunnelType(tunType).setDestinationIP(extIp).build();
        TunnelWorkerInterface externalTunnelToDCGW = new ItmExternalTunnelToDCGw(tunnelParameter);
        return externalTunnelToDCGW.buildTunnelFutureList();
//        return null;
    }

    public static List<ListenableFuture<Void>> buildHwVtepsTunnels(DataBroker dataBroker, IdManagerService idManagerService, List<DPNTEPsInfo> cfgdDpnList, List<HwVtep> cfgdHwVteps) {

        TunnelParameter tunnelParameter =  new TunnelParameter.Builder().setIdManagerService(idManagerService).setCfgdDpnList(cfgdDpnList).setCfgdHwVteps(cfgdHwVteps).
                                                    setDataBroker(dataBroker).setMonitorEnabled(ItmUtils.readMonitoringStateFromCache(dataBroker)).setMonitorInterval(ITMConstants.BFD_DEFAULT_MONITOR_INTERVAL).setMonitorProtocol(ITMConstants.DEFAULT_MONITOR_PROTOCOL).build();
        TunnelWorkerInterface buildHwTepTunnel = new ExternalTunnelToHwVTeps(tunnelParameter);
        return  buildHwTepTunnel.buildTunnelFutureList();
//        return  null;
    }

}

