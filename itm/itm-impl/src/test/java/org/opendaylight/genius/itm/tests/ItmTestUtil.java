/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.tests;


import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.itm.confighelpers.HwVtep;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.ExternalTunnelList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info.TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.TransportZone;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.Subnets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.SubnetsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.transport.zones.transport.zone.subnets.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ItmTestUtil {

    public static InstanceIdentifier<Interface> getInterfaceByKey(String IntKey) {
        return InstanceIdentifier.builder(Interfaces.class).child(Interface.class, new
                InterfaceKey(IntKey)).build();
    }

    public static org.opendaylight.yangtools.yang.binding.InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op
            .rev160406.external.tunnel.list.ExternalTunnel> getExternalTunnel(java.lang.String _destinationDevice, java.lang.String _sourceDevice,
                                                                              java.lang.Class<? extends org.opendaylight.yang.gen.v1.urn.opendaylight.genius.
                                                                                      interfacemanager.rev160406.TunnelTypeBase> _transportType) {
        return  InstanceIdentifier.create(ExternalTunnelList
                .class).child(ExternalTunnel.class, new ExternalTunnelKey( getExternalTunnelKey(_destinationDevice),
                getExternalTunnelKey(_sourceDevice.toString()), _transportType));
    }

    public static String getExternalTunnelKey(String nodeid) {
        if (nodeid.indexOf(ItmTestConstants.PHY_SWI_STR) > ItmTestConstants.ZERO) {
            nodeid = nodeid.substring(ItmTestConstants.ZERO, nodeid.indexOf(ItmTestConstants.PHY_SWI_STR) - ItmTestConstants.ONE);
        }
        return nodeid;
    }

    public static ListenableFuture<Void> writeInterfaceConfig(InstanceIdentifier<Interface> ifIID, Interface extTunnelIf, DataBroker dataBroker) throws ExecutionException, InterruptedException {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, ifIID, extTunnelIf);
        CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
        return future;
    }

    public static ListenableFuture<Void> writeExternalTunnelConfig(InstanceIdentifier<ExternalTunnel> externalTunnelIdentifier, ExternalTunnel externalTunnel, DataBroker dataBroker) {
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, externalTunnelIdentifier, externalTunnel);
        CheckedFuture<Void, TransactionCommitFailedException> future = tx.submit();
        return future;
    }

    public static List<DPNTEPsInfo> getDpnList(BigInteger dpId, int vlanId, String portName, IpAddress ipAddress, IpAddress gtwyIp, String parentInterfaceName,
                                               String transportZone, Class<? extends TunnelTypeBase> tunnelType, IpPrefix ipPrefixTest) {
        List<DPNTEPsInfo> dpnList = new ArrayList<>() ;
        dpnList.add(getdpntePsInfo(dpId,vlanId, portName, ipAddress, gtwyIp,  parentInterfaceName,
                transportZone, tunnelType, ipPrefixTest));
        return dpnList;
    }

    public static DPNTEPsInfo getdpntePsInfo(BigInteger dpId, int vlanId, String portName, IpAddress ipAddress, IpAddress gtwyIp, String parentInterfaceName,
                                             String transportZone, Class<? extends TunnelTypeBase> tunnelType, IpPrefix ipPrefixTest){

        List<TunnelEndPoints> tunnelEndPointsList = getTunnelEndPointsList(vlanId, portName, ipAddress, gtwyIp,  parentInterfaceName,
                transportZone, tunnelType, ipPrefixTest);

        return new DPNTEPsInfoBuilder().setDPNID(dpId).setUp(true).setKey(new DPNTEPsInfoKey(dpId))
               .setTunnelEndPoints(tunnelEndPointsList).build();

    }

    public static List<TunnelEndPoints> getTunnelEndPointsList(int vlanId, String portName, IpAddress ipAddress, IpAddress gtwyIp, String parentInterfaceName,
                                                               String transportZone, Class<? extends TunnelTypeBase> tunnelType, IpPrefix ipPrefixTest) {
        List<TunnelEndPoints> tunnelEndPointsList = new ArrayList<>();
        TunnelEndPoints tunnelEndPoints = getTunnelEndPoint( vlanId, portName, ipAddress, gtwyIp,  parentInterfaceName,
                 transportZone, tunnelType, ipPrefixTest);
        tunnelEndPointsList.add(tunnelEndPoints);
        return tunnelEndPointsList;
    }

    public static TunnelEndPoints getTunnelEndPoint(int vlanId,String portName,IpAddress ipAddress,IpAddress gtwyIp, String parentInterfaceName,
                                                    String transportZone,Class<? extends TunnelTypeBase> tunnelType,IpPrefix ipPrefixTest) {
          return new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName).setIpAddress
                (ipAddress).setGwIpAddress(gtwyIp).setInterfaceName(parentInterfaceName)
                .setTzMembership(ItmUtils.createTransportZoneMembership(transportZone))
                .setTunnelType(tunnelType).setSubnetMask(ipPrefixTest).build();


    }

    public static List<HwVtep> getHwVtepsList(String transportZone, IpAddress gtwyIp, IpAddress ipAddress, Class<? extends TunnelTypeBase> tunnelType, int vlanId, String source, IpPrefix ipPrefixTest) {
        List<HwVtep> cfgdHwVtepsList = new ArrayList<>();
        HwVtep hwVtep = getHwVtep(transportZone,gtwyIp,ipAddress,tunnelType,vlanId,source,ipPrefixTest);
        cfgdHwVtepsList.add(hwVtep);
        return cfgdHwVtepsList;
    }

    public static HwVtep getHwVtep(String transportZone,IpAddress gtwyIp,IpAddress ipAddress,Class<? extends TunnelTypeBase> tunnelType,int vlanId,String source,IpPrefix ipPrefixTest) {
        HwVtep hwVtep = new HwVtep();
        hwVtep.setTransportZone(transportZone);
        hwVtep.setGatewayIP(gtwyIp);
        hwVtep.setHwIp(ipAddress);
        hwVtep.setTunnel_type(tunnelType);
        hwVtep.setVlanID(vlanId);
        hwVtep.setTopo_id(ItmTestConstants.TOPO_ID);
        hwVtep.setNode_id(source);
        hwVtep.setIpPrefix(ipPrefixTest);
        return hwVtep;
    }

    public static String getFormattedString(Class<? extends TunnelTypeBase> tunnelType) {
        return String.format("%s %s", ItmUtils.convertTunnelTypetoString(tunnelType), ItmTestConstants.TRUNK_INT_STR);
    }

    public static Subnets getSubnet(List<Vteps> vtepList, IpAddress gwIp, IpPrefix ipPrefixTest, int vlanId, List<DeviceVteps> deviceVtepList) {
        return new SubnetsBuilder().setVteps(vtepList).setDeviceVteps(deviceVtepList).setGatewayIp(gwIp).setPrefix(ipPrefixTest).setVlanId(vlanId).setKey(new SubnetsKey(ipPrefixTest)).build();
    }

    public static Vteps getVtep(BigInteger dpnId, IpAddress ipAddress, boolean optionOfTunnel, String portName) {
        return new VtepsBuilder().setDpnId(dpnId).setIpAddress(ipAddress).setOptionOfTunnel(optionOfTunnel).setPortname(portName).build();
    }

    public static DeviceVteps getDeviceVtepList(IpAddress ipAddress, String topoId, String nodeId) {
        return new DeviceVtepsBuilder().setIpAddress(ipAddress).setKey(new DeviceVtepsKey(ipAddress, topoId))
                .setNodeId(nodeId).setTopologyId(topoId).build();
    }

    public static TransportZone getTransportZone(Class<? extends TunnelTypeBase> tunnelType, String tzName, List<Subnets> subnetsList) {
        return new TransportZoneBuilder().setTunnelType(tunnelType).setZoneName(tzName).setKey(new
                TransportZoneKey(tzName)).setSubnets(subnetsList).build();
    }
}
