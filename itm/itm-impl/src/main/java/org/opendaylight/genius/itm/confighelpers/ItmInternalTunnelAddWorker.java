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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.TunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.tunnel.list.InternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeLldp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class ItmInternalTunnelAddWorker {
     private static final Logger logger = LoggerFactory.getLogger(ItmInternalTunnelAddWorker.class) ;
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
        ITMBatchingUtils.update(dep,tnlBuilder , ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
    }

    private static void build_tunnel_from( DPNTEPsInfo srcDpn,List<DPNTEPsInfo> meshedDpnList, DataBroker dataBroker,  IdManagerService idManagerService, IMdsalApiManager mdsalManager, WriteTransaction t, List<ListenableFuture<Void>> futures) {
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
                                                   IdManagerService idManagerService, IMdsalApiManager mdsalManager,WriteTransaction t, List<ListenableFuture<Void>> futures) {
        logger.trace( "Wiring up within Transport Zone for Dpns {}, {} " , srcDpn, dstDpn );
        List<TunnelEndPoints> srcEndPts = srcDpn.getTunnelEndPoints();
        List<TunnelEndPoints> dstEndPts = dstDpn.getTunnelEndPoints();

        for( TunnelEndPoints srcte : srcEndPts) {
            for( TunnelEndPoints dstte : dstEndPts ) {
                // Compare the Transport zones
                if (!srcDpn.getDPNID().equals(dstDpn.getDPNID())) {
                    if( !ItmUtils.getIntersection(srcte.getTzMembership(), dstte.getTzMembership()).isEmpty()) {
                        // wire them up
                        wireUpBidirectionalTunnel( srcte, dstte, srcDpn.getDPNID(), dstDpn.getDPNID(), dataBroker, idManagerService,  mdsalManager, t, futures );
                        // CHECK THIS -- Assumption -- One end point per Dpn per transport zone
                        break ;
                    }
                }
            }
        }
    }

    private static void wireUpBidirectionalTunnel( TunnelEndPoints srcte, TunnelEndPoints dstte, BigInteger srcDpnId, BigInteger dstDpnId,
                                                   DataBroker dataBroker,  IdManagerService idManagerService, IMdsalApiManager mdsalManager, WriteTransaction t, List<ListenableFuture<Void>> futures) {
        // Setup the flow for LLDP monitoring -- PUNT TO CONTROLLER

        if(monitorProtocol.isAssignableFrom(TunnelMonitoringTypeLldp.class)) {
            ItmUtils.setUpOrRemoveTerminatingServiceTable(srcDpnId, mdsalManager, true);
            ItmUtils.setUpOrRemoveTerminatingServiceTable(dstDpnId, mdsalManager, true);
        }
        // Create the forward direction tunnel
        if(!wireUp( srcte, dstte, srcDpnId, dstDpnId, dataBroker, idManagerService, t, futures )) {
            logger.error("Could not build tunnel between end points {}, {} " , srcte, dstte );
        }

        // CHECK IF FORWARD IS NOT BUILT , REVERSE CAN BE BUILT
        // Create the tunnel for the reverse direction
        if(! wireUp( dstte, srcte, dstDpnId, srcDpnId, dataBroker, idManagerService, t, futures )) {
            logger.error("Could not build tunnel between end points {}, {} " , dstte, srcte);
        }
    }

    private static boolean wireUp(TunnelEndPoints srcte, TunnelEndPoints dstte, BigInteger srcDpnId, BigInteger dstDpnId ,
                                  DataBroker dataBroker, IdManagerService idManagerService, WriteTransaction t, List<ListenableFuture<Void>> futures) {
        // Wire Up logic
        logger.trace( "Wiring between source tunnel end points {}, destination tunnel end points {} " , srcte, dstte );
        String interfaceName = srcte.getInterfaceName() ;
        Class<? extends TunnelTypeBase> tunType = srcte.getTunnelType();
        String tunTypeStr = srcte.getTunnelType().getName();
        // Form the trunk Interface Name
        String trunkInterfaceName = ItmUtils.getTrunkInterfaceName( idManagerService, interfaceName,
                srcte.getIpAddress().getIpv4Address().getValue(),
                dstte.getIpAddress().getIpv4Address().getValue(),
                tunTypeStr) ;
        IpAddress gatewayIpObj = new IpAddress("0.0.0.0".toCharArray());
        IpAddress gwyIpAddress = srcte.getSubnetMask().equals(dstte.getSubnetMask()) ? gatewayIpObj : srcte.getGwIpAddress() ;
        logger.debug(  " Creating Trunk Interface with parameters trunk I/f Name - {}, parent I/f name - {}, source IP - {}, destination IP - {} gateway IP - {}",trunkInterfaceName, interfaceName, srcte.getIpAddress(), dstte.getIpAddress(), gwyIpAddress ) ;
        Interface iface = ItmUtils.buildTunnelInterface(srcDpnId, trunkInterfaceName, String.format( "%s %s",ItmUtils.convertTunnelTypetoString(srcte.getTunnelType()), "Trunk Interface"), true, tunType, srcte.getIpAddress(), dstte.getIpAddress(), gwyIpAddress, srcte.getVLANID(), true, monitorEnabled, monitorProtocol, monitorInterval);
        logger.debug(  " Trunk Interface builder - {} ", iface ) ;
        InstanceIdentifier<Interface> trunkIdentifier = ItmUtils.buildId(trunkInterfaceName);
        logger.debug(  " Trunk Interface Identifier - {} ", trunkIdentifier ) ;
        logger.trace(  " Writing Trunk Interface to Config DS {}, {} ", trunkIdentifier, iface ) ;
        ITMBatchingUtils.update(trunkIdentifier, iface, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
        ItmUtils.itmCache.addInterface(iface);
        // also update itm-state ds?
        InstanceIdentifier<InternalTunnel> path = InstanceIdentifier.create(
                TunnelList.class)
                .child(InternalTunnel.class, new InternalTunnelKey( dstDpnId, srcDpnId, tunType));
        InternalTunnel tnl = ItmUtils.buildInternalTunnel(srcDpnId, dstDpnId, tunType, trunkInterfaceName);
		// Switching to individual transaction submit as batching latencies is causing ELAN failures. Will revert when ELAN can handle this.
        //ITMBatchingUtils.update(path, tnl, ITMBatchingUtils.EntityType.DEFAULT_CONFIG);
		t.merge(LogicalDatastoreType.CONFIGURATION,path, tnl, true) ;
        ItmUtils.itmCache.addInternalTunnel(tnl);
        return true;
    }
}
