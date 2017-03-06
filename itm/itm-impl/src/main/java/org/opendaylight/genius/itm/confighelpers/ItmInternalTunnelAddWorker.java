/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeLogicalGroup;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmInternalTunnelAddWorker {
     private static final Logger logger = LoggerFactory.getLogger(ItmInternalTunnelAddWorker.class);

  private static Boolean monitorEnabled;
  private static Integer monitorInterval;
  private static Class<? extends TunnelMonitoringTypeBase> monitorProtocol;
  private static final FutureCallback<Void> DEFAULT_CALLBACK =
             new FutureCallback<Void>() {
                 @Override
                public void onSuccess(Void result) {
                     logger.debug("Success in Datastore operation");
                 }

                @Override
                public void onFailure(Throwable error) {
                    logger.error("Error in Datastore operation", error);
                }
             };


    public static List<ListenableFuture<Void>> build_all_tunnels(DataBroker dataBroker, IdManagerService idManagerService,IMdsalApiManager mdsalManager,
                                                                 List<DPNTEPsInfo> cfgdDpnList, List<DPNTEPsInfo> meshedDpnList) {
        logger.trace( "Building tunnels with DPN List {} " , cfgdDpnList );
        monitorInterval = ItmUtils.determineMonitorInterval(dataBroker);
        monitorProtocol = ItmUtils.determineMonitorProtocol(dataBroker);
        monitorEnabled = ItmUtils.readMonitoringStateFromCache(dataBroker);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        if( null == cfgdDpnList || cfgdDpnList.isEmpty()) {
            logger.error(" Build Tunnels was invoked with empty list");
            return futures;
        }

        for( DPNTEPsInfo dpn : cfgdDpnList) {
            //#####if dpn is not in meshedDpnList
            build_tunnel_from(dpn, meshedDpnList, dataBroker, idManagerService, mdsalManager, t, futures);
            if(null == meshedDpnList) {
                meshedDpnList = new ArrayList<>() ;
            }
            meshedDpnList.add(dpn) ;
            // Update the operational datastore -- FIXME -- Error Handling
            updateOperationalDatastore(dataBroker, dpn, t, futures) ;
        }
        futures.add( t.submit()) ;
        return futures ;
    }

    private static void updateOperationalDatastore(DataBroker dataBroker, DPNTEPsInfo dpn, WriteTransaction t, List<ListenableFuture<Void>> futures) {
        logger.debug("Updating CONFIGURATION datastore with DPN {} ", dpn);
        InstanceIdentifier<DpnEndpoints> dep = InstanceIdentifier.builder( DpnEndpoints.class).build() ;
        List<DPNTEPsInfo> dpnList = new ArrayList<>() ;
        dpnList.add(dpn) ;
        DpnEndpoints tnlBuilder = new DpnEndpointsBuilder().setDPNTEPsInfo(dpnList).build() ;
        t.merge(LogicalDatastoreType.CONFIGURATION, dep, tnlBuilder, true);
    }

    private static void build_tunnel_from( DPNTEPsInfo srcDpn,List<DPNTEPsInfo> meshedDpnList, DataBroker dataBroker,
            IdManagerService idManagerService, IMdsalApiManager mdsalManager,
            WriteTransaction t, List<ListenableFuture<Void>> futures) {
        logger.trace( "Building tunnels from DPN {} " , srcDpn );

        if( null == meshedDpnList || 0 == meshedDpnList.size()) {
            logger.debug( "No DPN in the mesh ");
            return ;
        }
        for( DPNTEPsInfo dstDpn: meshedDpnList) {
            if ( ! srcDpn.equals(dstDpn) ) {
                wireUpWithinTransportZone(srcDpn, dstDpn, dataBroker, idManagerService, mdsalManager, t, futures) ;
            }
        }

    }

    private static void wireUpWithinTransportZone( DPNTEPsInfo srcDpn, DPNTEPsInfo dstDpn, DataBroker dataBroker,
                                                   IdManagerService idManagerService, IMdsalApiManager mdsalManager,
                                                   WriteTransaction t, List<ListenableFuture<Void>> futures) {
        logger.trace( "Wiring up within Transport Zone for Dpns {}, {} " , srcDpn, dstDpn );
        List<TunnelEndPoints> srcEndPts = srcDpn.getTunnelEndPoints();
        List<TunnelEndPoints> dstEndPts = dstDpn.getTunnelEndPoints();

        for (TunnelEndPoints srcte : srcEndPts) {
            for (TunnelEndPoints dstte : dstEndPts) {
                // Compare the Transport zones
                if (!srcDpn.getDPNID().equals(dstDpn.getDPNID())) {
                    if (!ItmUtils.getIntersection(srcte.getTzMembership(), dstte.getTzMembership()).isEmpty()) {
                        // wire them up
                        wireUpBidirectionalTunnel( srcte, dstte, srcDpn.getDPNID(), dstDpn.getDPNID(), dataBroker,
                                idManagerService,  mdsalManager, t, futures);
                    }
                }
            }
        }
    }

    private static void wireUpBidirectionalTunnel( TunnelEndPoints srcte, TunnelEndPoints dstte,
                                                   BigInteger srcDpnId, BigInteger dstDpnId,
                                                   DataBroker dataBroker,  IdManagerService idManagerService,
                                                   IMdsalApiManager mdsalManager, WriteTransaction tx,
                                                   List<ListenableFuture<Void>> futures) {
        // Setup the flow for LLDP monitoring -- PUNT TO CONTROLLER

        if (monitorProtocol.isAssignableFrom(TunnelMonitoringTypeLldp.class)) {
            ItmUtils.setUpOrRemoveTerminatingServiceTable(srcDpnId, mdsalManager, true);
            ItmUtils.setUpOrRemoveTerminatingServiceTable(dstDpnId, mdsalManager, true);
        }
        // Create the forward direction tunnel
        if (!wireUp( srcte, dstte, srcDpnId, dstDpnId, dataBroker, idManagerService, mdsalManager, tx, futures)) {
            logger.error("Could not build tunnel between end points {}, {} " , srcte, dstte );
        }

        // CHECK IF FORWARD IS NOT BUILT , REVERSE CAN BE BUILT
        // Create the tunnel for the reverse direction
        if (!wireUp( dstte, srcte, dstDpnId, srcDpnId, dataBroker, idManagerService, mdsalManager, tx, futures)) {
            logger.error("Could not build tunnel between end points {}, {} " , dstte, srcte);
        }
    }

    private static boolean wireUp(TunnelEndPoints srcte, TunnelEndPoints dstte,
                                  BigInteger srcDpnId, BigInteger dstDpnId,
                                  DataBroker dataBroker, IdManagerService idManagerService,
                                  IMdsalApiManager mdsalManager, WriteTransaction t,
                                  List<ListenableFuture<Void>> futures) {
        // Wire Up logic
        logger.trace("Wiring between source tunnel end points {}, destination tunnel end points {}", srcte, dstte);
        String interfaceName = srcte.getInterfaceName();
        Class<? extends TunnelTypeBase> tunType = srcte.getTunnelType();
        String tunTypeStr = srcte.getTunnelType().getName();
        // Form the trunk Interface Name

        String trunkInterfaceName = ItmUtils.getTrunkInterfaceName(idManagerService, interfaceName,
                new String(srcte.getIpAddress().getValue()),
                new String(dstte.getIpAddress().getValue()),
                tunTypeStr);
        String parentInterfaceName = null;
        if (tunType.isAssignableFrom(TunnelTypeVxlan.class)) {
            parentInterfaceName = createLogicalGroupTunnel(srcte, dstte, srcDpnId, dstDpnId, dataBroker,
                                                           idManagerService, mdsalManager, t);
        }
        createTunnelInterace(srcte, dstte, srcDpnId, dstDpnId, tunType, trunkInterfaceName, parentInterfaceName, t);

        // also update itm-state ds?
        createInternalTunnel(srcDpnId, dstDpnId, tunType, trunkInterfaceName, t);
        return true;
    }

    private static void createTunnelInterace(TunnelEndPoints srcte, TunnelEndPoints dstte,
                                             BigInteger srcDpnId, BigInteger dstDpnId,
                                             Class<? extends TunnelTypeBase> tunType,
                                             String trunkInterfaceName, String parentInterfaceName,
                                             WriteTransaction tx) {
        String gateway = srcte.getIpAddress().getIpv4Address() != null ? "0.0.0.0" : "::";
        IpAddress gatewayIpObj = new IpAddress(gateway.toCharArray());
        IpAddress gwyIpAddress = srcte.getSubnetMask().equals(dstte.getSubnetMask()) ? gatewayIpObj : srcte.getGwIpAddress() ;
        logger.debug(" Creating Trunk Interface with parameters trunk I/f Name - {}, parent I/f name - {}, source IP - {}, destination IP - {} gateway IP - {}",
                trunkInterfaceName, srcte.getInterfaceName(), srcte.getIpAddress(), dstte.getIpAddress(), gwyIpAddress ) ;
        boolean useOfTunnel = ItmUtils.falseIfNull(srcte.isOptionOfTunnel());
        Interface iface = ItmUtils.buildTunnelInterface(srcDpnId, trunkInterfaceName,
                String.format("%s %s",ItmUtils.convertTunnelTypetoString(tunType), "Trunk Interface"),
                true, tunType, srcte.getIpAddress(), dstte.getIpAddress(), gwyIpAddress, srcte.getVLANID(), true,
                monitorEnabled, monitorProtocol, monitorInterval, useOfTunnel, parentInterfaceName);
        logger.debug(" Trunk Interface builder - {} ", iface);
        InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
        logger.debug(" Trunk Interface Identifier - {} ", trunkIdentifier);
        logger.trace(" Writing Trunk Interface to Config DS {}, {} ", trunkIdentifier, iface);
        tx.merge(LogicalDatastoreType.CONFIGURATION, trunkIdentifier, iface, true);
        ItmUtils.itmCache.addInterface(iface);
    }

    private static void createInternalTunnel(BigInteger srcDpnId, BigInteger dstDpnId,
                                             Class<? extends TunnelTypeBase> tunType,
                                             String trunkInterfaceName, WriteTransaction tx) {
        InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(
                TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey(dstDpnId, srcDpnId, tunType));
        InternalTunnel tnl = ItmUtils.buildInternalTunnel(srcDpnId, dstDpnId, tunType, trunkInterfaceName);
        tx.merge(LogicalDatastoreType.CONFIGURATION,path, tnl, true);
        ItmUtils.itmCache.addInternalTunnel(tnl);
    }

    private static String createLogicalGroupTunnel(TunnelEndPoints srcte, TunnelEndPoints dstte,
                                                   BigInteger srcDpnId, BigInteger dstDpnId,
                                                   DataBroker dataBroker, IdManagerService idManagerService,
                                                   IMdsalApiManager mdsalManager, WriteTransaction tx) {
        String logicTunnelGroupName = null;
        boolean tunnelAggregationEnabled = ItmTunnelAggregationConfigHelper.isTunnelAggregationEnabled();
        if (!tunnelAggregationEnabled) {
            logger.debug("MULTIPLE_VxLAN_TUNNELS: createLogicalGroupTunnel - not allowed configuration!");
            return logicTunnelGroupName;
        }
        String tunlTypeStr = ItmUtils.convertTunnelTypetoString(TunnelTypeLogicalGroup.class);
        logicTunnelGroupName = ItmUtils.getTrunkInterfaceName(idManagerService, srcte.getInterfaceName(),
                srcDpnId.toString(), dstDpnId.toString(), tunlTypeStr);
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        ItmTunnelAggregationWorker addWorker =
                new ItmTunnelAggregationWorker(logicTunnelGroupName, srcte, dstte, srcDpnId, dstDpnId, dataBroker);
        coordinator.enqueueJob(logicTunnelGroupName, addWorker);
        return logicTunnelGroupName;
    }

    private static class ItmTunnelAggregationWorker implements Callable<List<ListenableFuture<Void>>> {

        private final String logicTunnelGroupName;
        private final TunnelEndPoints srcTe;
        private final TunnelEndPoints dstTe;
        private final BigInteger srcDpnId;
        private final BigInteger dstDpnId;
        private final DataBroker dataBroker;

        public ItmTunnelAggregationWorker(String logicGroupName, TunnelEndPoints srcte, TunnelEndPoints dstte,
                BigInteger sDpnId, BigInteger dDpnId, DataBroker db) {
            logicTunnelGroupName = logicGroupName;
            srcTe = srcte;
            dstTe = dstte;
            srcDpnId = sDpnId;
            dstDpnId = dDpnId;
            dataBroker = db;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            //The logical tunnel interface be created only when the first tunnel interface on each OVS is created
            InternalTunnel tunnel = ItmUtils.itmCache.getInternalTunnel(logicTunnelGroupName);
            if (tunnel == null) {
                WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
                logger.debug("MULTIPLE_VxLAN_TUNNELS: add the logical tunnel group {} because a first tunnel"
                        + " interface on srcDpnId {}:{} dstDpnId {}:{} is created", logicTunnelGroupName, srcDpnId,
                        srcTe.getIpAddress(), dstDpnId, dstTe.getIpAddress());

                createTunnelInterace(srcTe, dstTe, srcDpnId, dstDpnId, TunnelTypeLogicalGroup.class,
                                     logicTunnelGroupName, null, tx);
                createInternalTunnel(srcDpnId, dstDpnId, TunnelTypeLogicalGroup.class, logicTunnelGroupName, tx);

                futures.add(tx.submit());
            } else {
                logger.debug("MULTIPLE_VxLAN_TUNNELS: not first tunnel on srcDpnId {} dstDpnId {}",srcDpnId, dstDpnId);
            }
            return futures;
        }
    }
}
