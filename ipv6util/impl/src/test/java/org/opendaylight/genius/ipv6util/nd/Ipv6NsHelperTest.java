/*
 * Copyright (c) 2017 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.ipv6util.nd;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class Ipv6NsHelperTest {
    private Ipv6NsHelper instance;
    private PacketProcessingService pktProcessService;

    @Before
    public void initTest() {
        pktProcessService = Mockito.mock(PacketProcessingService.class);
        instance = new Ipv6NsHelper(pktProcessService);
    }

    /**
     *  Test transmitNeighborSolicitation.
     */
    @Test
    public void testTransmitNeighborSolicitation() {
        doReturn(RpcResultBuilder.status(true).buildFuture()).when(pktProcessService)
            .transmitPacket(any(TransmitPacketInput.class));

        BigInteger dpnId = BigInteger.valueOf(1);
        String macAddr = "08:00:27:FE:8F:95";
        boolean retValue;
        Ipv6Address srcIpv6Address = new Ipv6Address("2001:db8::1");
        Ipv6Address targetIpv6Address = new Ipv6Address("2001:db8::2");
        InstanceIdentifier<Node> ncId = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1"))).build();
        NodeConnectorRef nodeRef = new NodeConnectorRef(ncId);
        retValue = instance.transmitNeighborSolicitation(dpnId, nodeRef, new MacAddress(macAddr),
                srcIpv6Address, targetIpv6Address);
        assertEquals(true, retValue);
        verify(pktProcessService, times(1)).transmitPacket(any(TransmitPacketInput.class));

        byte[] expectedPayload = buildPacket(
                "33 33 00 00 00 02",                               // Destination MAC
                "08 00 27 FE 8F 95",                               // Source MAC
                "86 DD",                                           // Ethertype - IPv6
                "60 00 00 00",                                     // Version 6, traffic class 0, no flowlabel
                "00 20",                                           // Payload length
                "3A",                                              // Next header is ICMPv6
                "FF",                                              // Hop limit
                "20 01 0D B8 00 00 00 00 00 00 00 00 00 00 00 01", // Source IP
                "FF 02 00 00 00 00 00 00 00 00 00 01 FF 00 00 02", // Destination IP
                "87",                                              // ICMPv6 neighbor advertisement.
                "00",                                              // Code
                "5E 94",                                           // Checksum (valid)
                "00 00 00 00",                                     // Flags
                "20 01 0D B8 00 00 00 00 00 00 00 00 00 00 00 02", // Target Address
                "01",                                              // Type: Source Link-Layer Option
                "01",                                              // Option length
                "08 00 27 FE 8F 95"                                // Source Link layer address
        );
        NodeConnectorRef nodeConnectorRef = MDSALUtil.getNodeConnRef(dpnId, "0xfffffffd");
        verify(pktProcessService).transmitPacket(new TransmitPacketInputBuilder().setPayload(expectedPayload)
                .setNode(new NodeRef(ncId)).setEgress(nodeRef).setIngress(nodeConnectorRef).build());
    }

    public byte[] buildPacket(String... contents) {
        List<String[]> splitContents = new ArrayList<>();
        int packetLength = 0;
        for (String content : contents) {
            String[] split = content.split(" ");
            packetLength += split.length;
            splitContents.add(split);
        }
        byte[] packet = new byte[packetLength];
        int index = 0;
        for (String[] split : splitContents) {
            for (String component : split) {
                // We can't use Byte.parseByte() here, it refuses anything > 7F
                packet[index] = (byte) Integer.parseInt(component, 16);
                index++;
            }
        }
        return packet;
    }
}
