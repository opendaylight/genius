/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.arputil.test;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.opendaylight.genius.arputil.test.ArpUtilTestUtil.INTERFACE_NAME;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opendaylight.genius.arputil.internal.ArpUtilImpl;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.infrautils.testutils.LogCaptureRule;
import org.opendaylight.infrautils.testutils.LogRule;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.GetMacInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.GetMacInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.GetMacOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpResponseInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpResponseInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.interfaces.InterfaceAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.interfaces.InterfaceAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class ArpUtilTest extends AbstractConcurrentDataBrokerTest {

    public @Rule LogRule logRule = new LogRule();
    public @Rule LogCaptureRule logCaptureRule = new LogCaptureRule();

    private ArpUtilImpl arpUtil;

    @Before
    public void setupTest() {
        arpUtil = new ArpUtilImpl(getDataBroker(), TestPacketProcessingService.newInstance(), mock(
            NotificationPublishService.class), mock(NotificationService.class),
            TestOdlInterfaceRpcService.newInstance(), mock(MetricProvider.class, RETURNS_DEEP_STUBS));
    }

    @Test
    public void testGetMac() throws Exception {
        final InterfaceAddress interfaceAddress = new InterfaceAddressBuilder()
                .setInterface(INTERFACE_NAME)
                .setIpAddress(new IpAddress(Ipv4Address.getDefaultInstance("192.168.0.1")))
                .setMacaddress(new PhysAddress("1F:1F:1F:1F:1F:1F")).build();

        final List<InterfaceAddress> itf = Collections.singletonList(interfaceAddress);

        GetMacInput getMacInput = new GetMacInputBuilder()
                .setIpaddress(new IpAddress(Ipv4Address.getDefaultInstance("192.168.0.2")))
                .setInterfaceAddress(itf).build();

        PacketReceived packetReceived = ArpUtilTestUtil.createPayload(0); //request payload

        Future<RpcResult<GetMacOutput>> output = arpUtil.getMac(getMacInput);

        arpUtil.onPacketReceived(packetReceived);

        Assert.assertEquals("00:01:02:03:04:05", output.get().getResult().getMacaddress().getValue());
    }

    @Test
    public void testSendArpResponse() throws Exception {
        SendArpResponseInput builder = new SendArpResponseInputBuilder().setInterface(INTERFACE_NAME)
                .setSrcIpaddress(new IpAddress(Ipv4Address.getDefaultInstance("192.168.0.1")))
                .setDstIpaddress(new IpAddress(Ipv4Address.getDefaultInstance("192.168.0.2")))
                .setSrcMacaddress(new PhysAddress("1F:1F:1F:1F:1F:1F"))
                .setDstMacaddress(new PhysAddress("00:01:02:03:04:05")).build();

        Assert.assertTrue(arpUtil.sendArpResponse(builder).get().isSuccessful());
    }
}
