/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.protocols.internal;

import static org.opendaylight.infrautils.utils.concurrent.LoggingFutures.addErrorLogging;

import java.util.Optional;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.genius.interfacemanager.globals.IfmConstants;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.actions.ActionOutput;
import org.opendaylight.genius.mdsalutil.actions.ActionSetFieldTunnelId;
import org.opendaylight.genius.mdsalutil.packet.Ethernet;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.openflowplugin.libraries.liblldp.EtherTypes;
import org.opendaylight.openflowplugin.libraries.liblldp.LLDP;
import org.opendaylight.openflowplugin.libraries.liblldp.LLDPTLV;
import org.opendaylight.openflowplugin.libraries.liblldp.LLDPTLV.TLVType;
import org.opendaylight.openflowplugin.libraries.liblldp.PacketException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.iana._if.type.rev170119.Tunnel;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfaceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProtocolType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.EndpointType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AlivenessProtocolHandlerLLDP extends AbstractAlivenessProtocolHandler<LLDP> {

    private static final Logger LOG = LoggerFactory.getLogger(AlivenessProtocolHandlerLLDP.class);

    // TODO org.opendaylight.openflowplugin.libraries.liblldp.LLDPTLV uses Charset.defaultCharset() .. bug there?
    private static final Charset LLDPTLV_CHARSET = StandardCharsets.US_ASCII;

    private final PacketProcessingService packetProcessingService;
    private final AtomicInteger packetId = new AtomicInteger(0);

    @Inject
    public AlivenessProtocolHandlerLLDP(
            @Reference final DataBroker dataBroker,
            @Reference final AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry,
            final PacketProcessingService packetProcessingService) {
        super(dataBroker, alivenessProtocolHandlerRegistry, MonitorProtocolType.Lldp);
        this.packetProcessingService = packetProcessingService;
    }

    @Override
    public Class<LLDP> getPacketClass() {
        return LLDP.class;
    }

    @Override
    // TODO remove DLS_DEAD_LOCAL_STORE when 1st  below lldpTlvTypeCur below is removed
    @SuppressFBWarnings({"DLS_DEAD_LOCAL_STORE", "NP_NONNULL_RETURN_VIOLATION"})
    public String handlePacketIn(LLDP lldpPacket, PacketReceived packetReceived) {
        String tempString = null;
        byte lldpTlvTypeCur;

        // TODO Remove? this seems completely pointless - lldpTlvTypeCur will get overwritten below..
        for (LLDPTLV lldpTlv : lldpPacket.getOptionalTLVList()) {
            lldpTlvTypeCur = lldpTlv.getType();
        }

        for (LLDPTLV lldpTlv : lldpPacket.getCustomTlvList()) {
            lldpTlvTypeCur = lldpTlv.getType();

            if (lldpTlvTypeCur == LLDPTLV.TLVType.Custom.getValue()) {
                tempString = new String(lldpTlv.getValue(), LLDPTLV_CHARSET);
            }
        }

        String interfaceName = null;

        // TODO: Check if the below fields are required
        if (!Strings.isNullOrEmpty(tempString) && tempString.contains("#")) {
            String[] asTmp = tempString.split("#");
            interfaceName = asTmp[0];
            LOG.debug("Custom LLDP Value on received packet: {}", tempString);
        }

        if (!Strings.isNullOrEmpty(interfaceName)) {
            String monitorKey = interfaceName + EtherTypes.LLDP;
            return monitorKey;
        } else {
            LOG.debug("No associated interface found to handle received LLDP Packet");
            return null;
        }
    }

    @Override
    public void startMonitoringTask(MonitoringInfo monitorInfo) {
        String sourceInterface;

        EndpointType source = monitorInfo.getSource().getEndpointType();
        if (source instanceof Interface) {
            Interface intf = (Interface) source;
            sourceInterface = intf.getInterfaceName();
        } else {
            LOG.warn("Invalid source endpoint. Could not retrieve source interface to send LLDP Packet");
            return;
        }

        // Get Mac Address for the source interface
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508
            .interfaces.state.Interface interfaceState;
        try {
            interfaceState = getInterfaceFromOperDS(sourceInterface);
        } catch (ReadFailedException e) {
            LOG.error("getInterfaceFromOperDS failed for sourceInterface: {}", sourceInterface, e);
            return;
        }

        Optional<byte[]> optSourceMac = getMacAddress(interfaceState);
        if (!optSourceMac.isPresent()) {
            LOG.error("Could not read mac address for the source interface {} from the Inventory. "
                    + "LLDP packet cannot be send.", sourceInterface);
            return;
        }
        byte[] sourceMac = optSourceMac.get();

        String lowerLayerIf = interfaceState.getLowerLayerIf().get(0);
        NodeConnectorId nodeConnectorId = new NodeConnectorId(lowerLayerIf);
        long nodeId = Long.parseLong(getDpnFromNodeConnectorId(nodeConnectorId));
        long portNum = Long.parseLong(getPortNoFromNodeConnectorId(nodeConnectorId));
        Ethernet ethenetLLDPPacket = makeLLDPPacket(Long.toString(nodeId), portNum, sourceMac, sourceInterface);

        try {
            List<ActionInfo> actions = getInterfaceActions(interfaceState, portNum);
            if (actions.isEmpty()) {
                LOG.error("No interface actions to send packet out over interface {}", sourceInterface);
                return;
            }
            TransmitPacketInput transmitPacketInput = MDSALUtil.getPacketOut(actions, ethenetLLDPPacket.serialize(),
                    nodeId, MDSALUtil.getNodeConnRef(Uint64.valueOf(nodeId), "0xfffffffd"));
            addErrorLogging(packetProcessingService.transmitPacket(transmitPacketInput),
                    LOG, "transmitPacket() failed: {}", transmitPacketInput);
        } catch (InterruptedException | ExecutionException | PacketException e) {
            LOG.error("Error while sending LLDP Packet", e);
        }
    }

    public static String getDpnFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        String[] split = portId.getValue().split(IfmConstants.OF_URI_SEPARATOR);
        return split[1];
    }

    public static String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
        /*
         * NodeConnectorId is of form 'openflow:dpnid:portnum'
         */
        String[] split = portId.getValue().split(IfmConstants.OF_URI_SEPARATOR);
        return split[2];
    }

    private static List<ActionInfo> getInterfaceActions(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .interfaces.rev140508.interfaces.state.Interface interfaceState, long portNum)
                    throws InterruptedException, ExecutionException {
        Class<? extends InterfaceType> intfType;
        if (interfaceState != null) {
            intfType = interfaceState.getType();
        } else {
            LOG.error("Could not retrieve port type for interface to construct actions");
            return Collections.emptyList();
        }

        List<ActionInfo> actionInfos = new ArrayList<>();
        // Set the LLDP service Id which is 0
        if (Tunnel.class.equals(intfType)) {
            actionInfos.add(new ActionSetFieldTunnelId(Uint64.ZERO));
        }
        actionInfos.add(new ActionOutput(new Uri(Long.toString(portNum))));
        return actionInfos;
    }

    @SuppressWarnings("AbbreviationAsWordInName")
    private static LLDPTLV buildLLDTLV(LLDPTLV.TLVType tlvType, byte[] abyTLV) {
        return new LLDPTLV().setType(tlvType.getValue()).setLength((short) abyTLV.length).setValue(abyTLV);
    }

    private int getPacketId() {
        int id = packetId.incrementAndGet();
        if (id > 16000) {
            LOG.debug("Resetting the LLDP Packet Id counter");
            packetId.set(0);
        }

        return id;
    }

    public Ethernet makeLLDPPacket(String nodeId, long portNum, byte[] srcMac, String sourceInterface) {

        // Create LLDP TTL TLV
        LLDPTLV lldpTlvTTL = buildLLDTLV(LLDPTLV.TLVType.TTL, new byte[] { (byte) 0, (byte) 120 });

        LLDPTLV lldpTlvChassisId = buildLLDTLV(LLDPTLV.TLVType.ChassisID, LLDPTLV.createChassisIDTLVValue(colonize(
                StringUtils.leftPad(Long.toHexString(MDSALUtil.getDpnIdFromNodeName(nodeId).longValue()), 16, "0"))));
        LLDPTLV lldpTlvSystemName = buildLLDTLV(TLVType.SystemName, LLDPTLV.createSystemNameTLVValue(nodeId));

        LLDPTLV lldpTlvPortId = buildLLDTLV(TLVType.PortID, LLDPTLV.createPortIDTLVValue(Long.toHexString(portNum)));

        String customValue = sourceInterface + "#" + getPacketId();

        LOG.debug("Sending LLDP packet, custome value {}", customValue);

        LLDPTLV lldpTlvCustom = buildLLDTLV(TLVType.Custom, customValue.getBytes(LLDPTLV_CHARSET));

        @SuppressWarnings("AbbreviationAsWordInName")
        List<LLDPTLV> lstLLDPTLVCustom = new ArrayList<>();
        lstLLDPTLVCustom.add(lldpTlvCustom);

        LLDP lldpDiscoveryPacket = new LLDP();
        lldpDiscoveryPacket.setChassisId(lldpTlvChassisId).setPortId(lldpTlvPortId).setTtl(lldpTlvTTL)
                .setSystemNameId(lldpTlvSystemName).setOptionalTLVList(lstLLDPTLVCustom);

        byte[] destMac = LLDP.LLDP_MULTICAST_MAC;

        Ethernet ethernetPacket = new Ethernet();
        ethernetPacket.setSourceMACAddress(srcMac).setDestinationMACAddress(destMac)
                .setEtherType(EtherTypes.LLDP.shortValue()).setPayload(lldpDiscoveryPacket);

        return ethernetPacket;
    }

    private static String colonize(String orig) {
        return orig.replaceAll("(?<=..)(..)", ":$1");
    }

    @Override
    public String getUniqueMonitoringKey(MonitoringInfo monitorInfo) {
        String interfaceName = getInterfaceName(monitorInfo.getSource().getEndpointType());
        return interfaceName + EtherTypes.LLDP;
    }

    private static String getInterfaceName(EndpointType endpoint) {
        return endpoint instanceof Interface ? ((Interface) endpoint).getInterfaceName() : null;
    }
}
