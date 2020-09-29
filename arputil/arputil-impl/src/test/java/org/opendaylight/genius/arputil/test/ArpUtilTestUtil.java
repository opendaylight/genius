/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.arputil.test;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.SendToController;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.packet.received.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class ArpUtilTestUtil {

    public static final Uint64 DPN_ID = Uint64.ONE;
    public static final Uint64 META_DATA = Uint64.TEN;
    public static final Uint32 PORT_NUMBER = Uint32.TWO;
    public static final String URI = "2";
    public static final String INTERFACE_NAME = "23701c04-7e58-4c65-9425-78a80d49a218";
    private static final String[] OP_CODE = new String[]{"0 1", "0 2"}; //array to store opCodes

    private ArpUtilTestUtil() {
    }

    public static PacketReceived createPayload(int oc) {

        byte[] payload = bytePayload("1F 1F 1F 1F 1F 1F",                               // Destination MAC
                                     "00 01 02 03 04 05",                               // Source MAC
                                     "08 06",                                           // Ethernet type
                                     "0 1",                                             // Hardware type
                                     "8 0",                                             // Protocol type
                                     "6",                                               // Hardware size
                                     "4",                                               // Protocol size
                                     OP_CODE[oc],                                       // Opcode
                                     "00 01 02 03 04 05",                               // Sender MAC Address
                                     "C0 A8 0 2",                                       // Sender IP Address
                                     "00 01 02 03 04 05",                               // Target MAC Address
                                     "C0 A8 0 2"                                        // Target IP Address
        );
        InstanceIdentifier<Node> iid = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:12345"))).build();

        return new PacketReceivedBuilder().setPacketInReason(SendToController.class).setTableId(new TableId(Uint8.TWO))
                .setPayload(payload).setIngress(new NodeConnectorRef(iid))
                .setMatch(new MatchBuilder().setMetadata(new MetadataBuilder().setMetadata(META_DATA).build()).build())
                .build();
    }

    private static byte[] bytePayload(String... contents) {

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
                packet[index] = (byte) Integer.parseInt(component, 16);
                index++;
            }
        }
        return packet;
    }
}
