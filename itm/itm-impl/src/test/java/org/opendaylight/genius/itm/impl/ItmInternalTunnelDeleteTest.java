/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.impl;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Futures;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.confighelpers.ItmInternalTunnelDeleteWorker;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpPrefixBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev150403.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemgr.rev150331.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemgr.rev150331.TunnelTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnEndpointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info
        .TunnelEndPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info
        .TunnelEndPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.dpn.teps.info
        .TunnelEndPointsKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ItmInternalTunnelDeleteTest {

    BigInteger dpId1 = BigInteger.valueOf(1);
    BigInteger dpId2 = BigInteger.valueOf(2);
    int vlanId = 100 ;
    String portName1 = "phy0";
    String portName2 = "phy1" ;
    String parentInterfaceName = "1:phy0:100" ;
    String transportZone1 = "TZA" ;
    String subnetIp = "10.1.1.24";
    String tepIp1 = "192.168.56.101";
    String tepIp2 = "192.168.56.102";
    String gwyIp1 = "0.0.0.0";
    String gwyIp2 = "0.0.0.1";
    IpAddress ipAddress1 = null;
    IpAddress ipAddress2 = null;
    IpAddress gtwyIp1 = null;
    IpAddress gtwyIp2 = null;
    IpPrefix ipPrefixTest = null;
    DPNTEPsInfo dpntePsInfoVxlan = null;
    DPNTEPsInfo dpntePsInfoVxlanNew = null;
    TunnelEndPoints tunnelEndPointsVxlan = null;
    TunnelEndPoints tunnelEndPointsVxlanNew = null;
    DpnEndpoints dpnEndpoints = null;
    List<DPNTEPsInfo> meshDpnListVxlan = new ArrayList<DPNTEPsInfo>() ;
    List<DPNTEPsInfo> cfgdDpnListVxlan = new ArrayList<DPNTEPsInfo>() ;
    List<TunnelEndPoints> tunnelEndPointsListVxlan = new ArrayList<>();
    List<TunnelEndPoints> tunnelEndPointsListVxlanNew = new ArrayList<>();
    java.lang.Class<? extends TunnelTypeBase> tunnelType1 = TunnelTypeVxlan.class;

    @Mock DataBroker dataBroker;
    @Mock ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;
    @Mock ReadOnlyTransaction mockReadTx;
    @Mock WriteTransaction mockWriteTx;
    @Mock IdManagerService idManagerService;
    @Mock IMdsalApiManager iMdsalApiManager;

    ItmInternalTunnelDeleteWorker itmInternalTunnelDeleteWorker = new ItmInternalTunnelDeleteWorker();

    @Before
    public void setUp() throws Exception {
        when(dataBroker.registerDataChangeListener(
                any(LogicalDatastoreType.class),
                any(InstanceIdentifier.class),
                any(DataChangeListener.class),
                any(AsyncDataBroker.DataChangeScope.class)))
                .thenReturn(dataChangeListenerRegistration);
        setupMocks();
    }

    @After
    public void cleanUp() {
    }

    private void setupMocks(){

        ipAddress1 = IpAddressBuilder.getDefaultInstance(tepIp1);
        ipAddress2 = IpAddressBuilder.getDefaultInstance(tepIp2);
        ipPrefixTest = IpPrefixBuilder.getDefaultInstance(subnetIp + "/24");
        gtwyIp1 = IpAddressBuilder.getDefaultInstance(gwyIp1);
        gtwyIp2 = IpAddressBuilder.getDefaultInstance(gwyIp2);
        tunnelEndPointsVxlan = new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName1).setIpAddress
                (ipAddress1).setGwIpAddress(gtwyIp1).setInterfaceName(parentInterfaceName).setTransportZone
                (transportZone1).setTunnelType(tunnelType1).setSubnetMask(ipPrefixTest).setKey(new TunnelEndPointsKey
                (ipAddress1,portName1,tunnelType1,vlanId)).build();
        tunnelEndPointsVxlanNew = new TunnelEndPointsBuilder().setVLANID(vlanId).setPortname(portName2).setIpAddress
                (ipAddress2).setGwIpAddress(gtwyIp2).setInterfaceName(parentInterfaceName).setTransportZone
                (transportZone1).setTunnelType(tunnelType1).setSubnetMask(ipPrefixTest).build();
        tunnelEndPointsListVxlan.add(tunnelEndPointsVxlan);
        tunnelEndPointsListVxlanNew.add(tunnelEndPointsVxlanNew);
        dpntePsInfoVxlan = new DPNTEPsInfoBuilder().setDPNID(dpId1).setKey(new DPNTEPsInfoKey(dpId1)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        dpntePsInfoVxlanNew = new DPNTEPsInfoBuilder().setDPNID(dpId2).setKey(new DPNTEPsInfoKey(dpId1)).setUp(true)
                .setTunnelEndPoints(tunnelEndPointsListVxlanNew).setTunnelEndPoints(tunnelEndPointsListVxlan).build();
        cfgdDpnListVxlan.add(dpntePsInfoVxlan);
        meshDpnListVxlan.add(dpntePsInfoVxlanNew);
        dpnEndpoints = new DpnEndpointsBuilder().setDPNTEPsInfo(cfgdDpnListVxlan).build();

        doReturn(mockReadTx).when(dataBroker).newReadOnlyTransaction();
        doReturn(mockWriteTx).when(dataBroker).newWriteOnlyTransaction();
        doReturn(Futures.immediateCheckedFuture(null)).when(mockWriteTx).submit();

    }

    @Test
    public void testDeleteTunnels(){

        InstanceIdentifier<TunnelEndPoints> tunnelEndPointsIdentifier =
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, dpntePsInfoVxlan.getKey())
                        .child(TunnelEndPoints.class,tunnelEndPointsVxlan.getKey() ).build();
        InstanceIdentifier<DpnEndpoints> dpnEndpointsIdentifier =
                InstanceIdentifier.builder(DpnEndpoints.class).build();
        InstanceIdentifier<DPNTEPsInfo> dpntePsInfoIdentifier =
                InstanceIdentifier.builder(DpnEndpoints.class).child(DPNTEPsInfo.class, dpntePsInfoVxlan.getKey()).build();

        Optional<DpnEndpoints> dpnEndpointsOptional = Optional.of(dpnEndpoints);

        doReturn(Futures.immediateCheckedFuture(dpnEndpointsOptional)).when(mockReadTx).read(LogicalDatastoreType
                .CONFIGURATION,dpnEndpointsIdentifier);

        itmInternalTunnelDeleteWorker.deleteTunnels(dataBroker,idManagerService,iMdsalApiManager,cfgdDpnListVxlan,
                meshDpnListVxlan);

        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,tunnelEndPointsIdentifier);
        verify(mockWriteTx).delete(LogicalDatastoreType.CONFIGURATION,dpntePsInfoIdentifier);

    }
}
