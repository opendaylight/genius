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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.impl.TunnelMonitoringConfig;
import org.opendaylight.genius.itm.scaling.renderer.ovs.confighelpers.OvsTunnelConfigAddHelper;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TepInternalTunnelAddWorker {
    private static final Logger LOG = LoggerFactory.getLogger(TepInternalTunnelAddWorker.class);
    private static Boolean monitorEnabled;
    private static Integer monitorInterval;
    private static Class<? extends TunnelMonitoringTypeBase> monitorProtocol;

    private TepInternalTunnelAddWorker() {
    }

    public static List<ListenableFuture<Void>> buildAllTunnels(DataBroker dataBroker,
                                                               IMdsalApiManager mdsalManager,
                                                               IdManagerService idManagerService,
                                                               List<DPNTEPsInfo> cfgdDpnList,
                                                               Collection<DPNTEPsInfo> meshedDpnList,
                                                               TunnelMonitoringConfig tunnelMonitoringConfig) {
        LOG.trace("Building tunnels with DPN List {} ", cfgdDpnList);
        monitorInterval = tunnelMonitoringConfig.getMonitorInterval();
        monitorProtocol = tunnelMonitoringConfig.getMonitorProtocol();
        monitorEnabled = tunnelMonitoringConfig.isTunnelMonitoringEnabled();
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        if (null == cfgdDpnList || cfgdDpnList.isEmpty()) {
            LOG.error("Build Tunnels was invoked with empty list");
            return futures;
        }

        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        for (DPNTEPsInfo dpn : cfgdDpnList) {
            buildTunnelFrom(dpn, meshedDpnList, dataBroker, mdsalManager, idManagerService, transaction);
            if (null == meshedDpnList) {
                meshedDpnList = new ArrayList<>();
            }
            meshedDpnList.add(dpn);
        }
        if (meshedDpnList.size() == 1) {
            DPNTEPsInfo dpn = cfgdDpnList.get(0);
            updateDpnTepInfoToConfig(dpn, transaction);
        }

        futures.add(transaction.submit()) ;
        return futures ;
    }

    private static void updateDpnTepInfoToConfig(DPNTEPsInfo dpn, WriteTransaction transaction) {
        LOG.debug("Updating CONFIGURATION datastore with DPN {} ", dpn);
        InstanceIdentifier<DpnEndpoints> dep = InstanceIdentifier.builder(DpnEndpoints.class).build() ;
        List<DPNTEPsInfo> dpnList = new ArrayList<>() ;
        dpnList.add(dpn) ;
        DpnEndpoints tnlBuilder = new DpnEndpointsBuilder().setDPNTEPsInfo(dpnList).build() ;
        ITMBatchingUtils.update(dep, tnlBuilder, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
    }

    private static void updateDpnTepInterfaceInfoToConfig(DpnTepsState dpnTeps, WriteTransaction transaction) {
        LOG.debug("Updating CONFIGURATION datastore with DPN-Teps {} ", dpnTeps);
        InstanceIdentifier<DpnTepsState> dpnTepsII = InstanceIdentifier.builder(DpnTepsState.class).build() ;
        ITMBatchingUtils.update(dpnTepsII, dpnTeps, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
    }

    private static void buildTunnelFrom(DPNTEPsInfo srcDpn, Collection<DPNTEPsInfo> meshedDpnList,
                                        DataBroker dataBroker,
                                        IMdsalApiManager mdsalManager,
                                        IdManagerService idManagerService,
                                        WriteTransaction transaction) {
        LOG.trace("Building tunnels from DPN {} " , srcDpn);
        if (null == meshedDpnList || meshedDpnList.isEmpty()) {
            LOG.debug("No DPN in the mesh ");
            return ;
        }
        for (DPNTEPsInfo dstDpn: meshedDpnList) {
            if (!srcDpn.equals(dstDpn)) {
                wireUpWithinTransportZone(srcDpn, dstDpn, dataBroker, mdsalManager,
                        idManagerService, transaction);
            }
        }

    }

    private static void wireUpWithinTransportZone(DPNTEPsInfo srcDpn, DPNTEPsInfo dstDpn,
                                                  DataBroker dataBroker, IMdsalApiManager mdsalManager,
                                                  IdManagerService idManagerService, WriteTransaction transaction) {
        LOG.trace("Wiring up within Transport Zone for Dpns {}, {} " , srcDpn, dstDpn);
        List<TunnelEndPoints> srcEndPts = srcDpn.getTunnelEndPoints();
        List<TunnelEndPoints> dstEndPts = dstDpn.getTunnelEndPoints();

        for (TunnelEndPoints srcte : srcEndPts) {
            for (TunnelEndPoints dstte : dstEndPts) {
                // Compare the Transport zones
                if (!srcDpn.getDPNID().equals(dstDpn.getDPNID())) {
                    if (!ItmUtils.getIntersection(srcte.getTzMembership(), dstte.getTzMembership()).isEmpty()) {
                        // wire them up
                        wireUpBidirectionalTunnel(srcte, dstte, srcDpn, dstDpn, dataBroker,
                                mdsalManager, idManagerService, transaction);
                    }
                }
            }
        }
    }

    private static void wireUpBidirectionalTunnel(TunnelEndPoints srcte, TunnelEndPoints dstte, DPNTEPsInfo srcDpn,
                                                  DPNTEPsInfo dstDpn, DataBroker dataBroker,
                                                  IMdsalApiManager mdsalManager, IdManagerService idManagerService,
                                                  WriteTransaction transaction) {
        // Setup the flow for LLDP monitoring -- PUNT TO CONTROLLER

        if (monitorProtocol.isAssignableFrom(TunnelMonitoringTypeLldp.class)) {
            ItmUtils.setUpOrRemoveTerminatingServiceTable(srcDpn.getDPNID(), mdsalManager, true);
            ItmUtils.setUpOrRemoveTerminatingServiceTable(dstDpn.getDPNID(), mdsalManager, true);
        }
        // Create the forward direction tunnel
        if (!wireUp(srcte, dstte, srcDpn, dstDpn, dataBroker,
                idManagerService, transaction)) {
            LOG.error("Could not build tunnel between end points {}, {} " , srcte, dstte);
        }

        // CHECK IF FORWARD IS NOT BUILT , REVERSE CAN BE BUILT
        // Create the tunnel for the reverse direction
        if (!wireUp(dstte, srcte, dstDpn, srcDpn, dataBroker,
                idManagerService, transaction)) {
            LOG.error("Could not build tunnel between end points {}, {} " , dstte, srcte);
        }
    }

    private static boolean wireUp(TunnelEndPoints srcte, TunnelEndPoints dstte,
                                  DPNTEPsInfo srcDpn, DPNTEPsInfo dstDpn,
                                  DataBroker dataBroker, IdManagerService idManagerService,
                                  WriteTransaction transaction) {
        // Wire Up logic
        LOG.trace("Wiring between source tunnel end points {}, destination tunnel end points {}", srcte, dstte);
        String interfaceName = srcte.getInterfaceName() ;
        Class<? extends TunnelTypeBase> tunType = srcte.getTunnelType();
        String tunTypeStr = srcte.getTunnelType().getName();
        // Form the trunk Interface Name
        String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(interfaceName,
                srcte.getIpAddress().getIpv4Address().getValue(),
                dstte.getIpAddress().getIpv4Address().getValue(),
                tunTypeStr);
        IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
        IpAddress gwyIpAddress = srcte.getSubnetMask().equals(dstte.getSubnetMask())
                ? gatewayIpObj : srcte.getGwIpAddress() ;
        LOG.debug("Creating Trunk Interface with parameters trunk I/f Name - {}, parent I/f name - {}, source IP - {},"
                + " destination IP - {} gateway IP - {}", trunkInterfaceName, interfaceName, srcte.getIpAddress(),
                dstte.getIpAddress(), gwyIpAddress) ;

        Interface iface = ItmUtils.buildTunnelInterface(srcDpn.getDPNID(), trunkInterfaceName,
                String.format("%s %s",ItmUtils.convertTunnelTypetoString(srcte.getTunnelType()), "Trunk Interface"),
                true, tunType, srcte.getIpAddress(), dstte.getIpAddress(), gwyIpAddress, srcte.getVLANID(),
                true, monitorEnabled, monitorProtocol, monitorInterval, false, null);
        LOG.debug("Trunk Interface builder - {} ", iface);
        // Assign the parentRef
        // Configure tunnel here. This could be tunnel ports or OF tunnels.ParentRef will be used there
        // TODO :- logical group support
        LOG.debug("Storig DPN TEPs INfo ", srcDpn);
        updateDpnTepInfoToConfig(srcDpn, transaction);

        LOG.debug("Building tunnels with DPN ", srcDpn.getDPNID());
        final DpnTepsStateBuilder dpnTepsStateBuilder = new DpnTepsStateBuilder();
        final DpnsTepsBuilder  dpnsTepsBuilder = new DpnsTepsBuilder();
        final List<DpnsTeps> dpnTeps = new ArrayList<DpnsTeps>();
        final List<RemoteDpns> remoteDpns = new ArrayList<RemoteDpns>();
        dpnsTepsBuilder.setKey(new DpnsTepsKey(srcDpn.getDPNID()));
        dpnsTepsBuilder.setTunnelType(srcte.getTunnelType());
        dpnsTepsBuilder.setSourceDpnId(srcDpn.getDPNID());

        //ITM TEP INTERFACE set the group Id here ..later
        Integer groupId =  ItmUtils.allocateId(idManagerService, ITMConstants.ITM_IDPOOL_NAME,
                srcDpn.getDPNID().toString());
        dpnsTepsBuilder.setGroupId(groupId.longValue());

        RemoteDpnsBuilder remoteDpn = new RemoteDpnsBuilder();
        remoteDpn.setKey(new RemoteDpnsKey(dstDpn.getDPNID()));
        remoteDpn.setDestinationDpnId(dstDpn.getDPNID());
        remoteDpn.setTunnelName(trunkInterfaceName);
        remoteDpn.setMonitoringEnabled(monitorEnabled);
        remoteDpn.setMonitoringInterval(monitorInterval);
        remoteDpn.setInternal(true);
        remoteDpns.add(remoteDpn.build());
        dpnsTepsBuilder.setRemoteDpns(remoteDpns);
        dpnTeps.add(dpnsTepsBuilder.build());
        dpnTepsStateBuilder.setDpnsTeps(dpnTeps);
        LOG.debug("Storig DPN TEPs State ", dpnTeps);
        updateDpnTepInterfaceInfoToConfig(dpnTepsStateBuilder.build(), transaction);
        OvsTunnelConfigAddHelper.addTunnelConfiguration(dataBroker, iface);
        return true;
    }

}
