/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.arputil.test;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev140508.L2vlan;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.Interfaces;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.Interface;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfL2vlanBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.SendToController;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.packet.received.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.table.types.rev131026.TableId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArpUtilTestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(ArpUtilTestUtil.class);
    public static final BigInteger DPN_ID = BigInteger.ONE;
    public static final BigInteger META_DATA  =  BigInteger.TEN;
    public static final long PORT_NUMBER = Long.valueOf(2);
    public static final String URI = "2";
    public static final String INTERFACE_NAME = "23701c04-7e58-4c65-9425-78a80d49a218";

    static void putInterfaceConfig(DataBroker dataBroker, String ifaceName, ParentRefs parentRefs,
                                   Class<? extends InterfaceType> ifType) throws TransactionCommitFailedException {
        Interface interfaceInfo;
        interfaceInfo = buildInterface(ifaceName, ifaceName, true, ifType,
                    parentRefs.getParentInterface(), IfL2vlan.L2vlanMode.Trunk);
        InstanceIdentifier<Interface> interfaceInstanceIdentifier = buildId(ifaceName);
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(CONFIGURATION, interfaceInstanceIdentifier, interfaceInfo, true);
        tx.submit().checkedGet();
    }

    public static InstanceIdentifier<Interface> buildId(String interfaceName) {
        // TODO Make this generic and move to AbstractDataChangeListener or
        // Utils.
        InstanceIdentifier.InstanceIdentifierBuilder<Interface> idBuilder = InstanceIdentifier.builder(Interfaces.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        return idBuilder.build();
    }

    static Interface buildInterface(String ifName, String desc, boolean enabled, Object ifType,
                                    String parentInterface, IfL2vlan.L2vlanMode l2vlanMode) {
        InterfaceBuilder builder = new InterfaceBuilder().setKey(new InterfaceKey(ifName)).setName(ifName)
                .setDescription(desc).setEnabled(enabled).setType((Class<? extends InterfaceType>) ifType);
        ParentRefs parentRefs = new ParentRefsBuilder().setParentInterface(parentInterface).build();
        builder.addAugmentation(ParentRefs.class, parentRefs);
        if (ifType.equals(L2vlan.class)) {
            IfL2vlanBuilder ifL2vlanBuilder = new IfL2vlanBuilder().setL2vlanMode(l2vlanMode);
            if (IfL2vlan.L2vlanMode.TrunkMember.equals(l2vlanMode)) {
                ifL2vlanBuilder.setVlanId(new VlanId(100));
            } else {
                ifL2vlanBuilder.setVlanId(VlanId.getDefaultInstance("0"));
            }
            builder.addAugmentation(IfL2vlan.class, ifL2vlanBuilder.build());
        }
        return builder.build();
    }

    public static void addStateEntry(String interfaceName, DataBroker dataBroker,
                                     org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                             .ietf.interfaces.rev140508.interfaces.state.Interface ifState) {

        final WriteTransaction interfaceOperShardTransaction = dataBroker.newWriteOnlyTransaction();
        Integer ifIndex = 1;
        LOG.debug("adding interface state for {}", interfaceName);
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus operStatus = org.opendaylight.yang
                .gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.OperStatus.Up;
        PhysAddress physAddress = ifState.getPhysAddress();
        NodeConnectorId nodeConnectorId = new NodeConnectorId(ifState.getLowerLayerIf().get(0));

        List<String> childLowerLayerIfList = new ArrayList<>();
        childLowerLayerIfList.add(0, nodeConnectorId.getValue());
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder ifaceBuilder =
                new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.InterfaceBuilder()
                .setAdminStatus(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces
                        .rev140508.interfaces.state.Interface.AdminStatus.Up).setOperStatus(operStatus)
                .setPhysAddress(physAddress).setLowerLayerIf(childLowerLayerIfList);
        ifaceBuilder.setIfIndex(ifIndex);
        ifaceBuilder.setType(L2vlan.class);

        ifaceBuilder.setKey(new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                .interfaces.state.InterfaceKey(interfaceName));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface> ifStateId = buildStateInterfaceId(interfaceName);
        interfaceOperShardTransaction.put(LogicalDatastoreType.OPERATIONAL, ifStateId, ifaceBuilder.build(), true);
        interfaceOperShardTransaction.submit();
    }

    public static InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
            .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface> buildStateInterfaceId(
            String interfaceName) {
        InstanceIdentifierBuilder<org.opendaylight.yang.gen.v1.urn
                .ietf.params.xml.ns.yang.ietf.interfaces.rev140508
                .interfaces.state.Interface> idBuilder = InstanceIdentifier
                .builder(InterfacesState.class)
                .child(org.opendaylight.yang.gen.v1.urn
                                .ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface.class,
                        new org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                                .ietf.interfaces.rev140508.interfaces.state.InterfaceKey(
                                interfaceName));
        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang
                .ietf.interfaces.rev140508.interfaces.state.Interface> id = idBuilder
                .build();
        return id;
    }

    public static PacketReceived createPayload() {

        final short ID = 2;
        byte [] payload = bytePayload(
                "1F 1F 1F 1F 1F 1F",                               // Destination MAC
                "00 01 02 03 04 05",                               // Source MAC
                "08 06",                                           // Ethernet type
                "0 8",                                             // Hardware type
                "0 0",                                             // Protocol type
                "6",                                               // Hardware size
                "4",                                               // Protocol size
                "0 2",                                             // Opcode
                "00 01 02 03 04 05",                               // Sender MAC Address
                "192 169 0 2",                                     // Sender IP Address
                "1F 1F 1F 1F 1F 1F",                               // Target MAC Address
                "192 168 0 1"                                      // Target IP Address
                );

        return new PacketReceivedBuilder().setPacketInReason(SendToController.class)
                .setTableId(new TableId(ID)).setPayload(payload)
                .setIngress(new NodeConnectorRef(InstanceIdentifier.create(Node.class)))
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
