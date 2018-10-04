/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.alivenessmonitor.protocols.internal;

import static org.opendaylight.genius.alivenessmonitor.protocols.AlivenessMonitorAndProtocolsConstants.SEPERATOR;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.genius.alivenessmonitor.utils.AlivenessMonitorUtil;
import org.opendaylight.genius.ipv6util.api.Ipv6Util;
import org.opendaylight.genius.ipv6util.api.decoders.Ipv6NaDecoder;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.packet.utils.PacketUtil;
import org.opendaylight.openflowplugin.libraries.liblldp.BufferException;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.EtherTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.EndpointType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.packet.rev160620.NeighborAdvertisePacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.Ipv6NdUtilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.SendNeighborSolicitationInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.SendNeighborSolicitationInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.SendNeighborSolicitationOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.interfaces.InterfaceAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.interfaces.InterfaceAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlivenessProtocolHandlerIPv6ND extends AbstractAlivenessProtocolHandler<Packet> {

    private static final Logger LOG = LoggerFactory.getLogger(AlivenessProtocolHandlerIPv6ND.class);

    private final Ipv6NdUtilService ndService;
    private final OdlInterfaceRpcService interfaceManager;

    public AlivenessProtocolHandlerIPv6ND(final DataBroker dataBroker, final OdlInterfaceRpcService interfaceManager,
            final AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry,
            final Ipv6NdUtilService ndService) {
        super(dataBroker, alivenessProtocolHandlerRegistry, EtherTypes.Ipv6Nd);
        this.interfaceManager = interfaceManager;
        this.ndService = ndService;
        LOG.trace("AlivenessProtocolHandlerIPv6ND constructor called.");
    }

    @Override
    public Class<Packet> getPacketClass() {
        return Packet.class;
    }

    @Override
    public String handlePacketIn(Packet packet, PacketReceived packetReceived) {
        // parameter "packet" is expected to be null in this case
        byte[] data = packetReceived.getPayload();
        if (!PacketUtil.isIpv6NaPacket(data)) {
            LOG.warn("Packet received is not an IPv6 NA packet, ignored.");
            return null;
        }

        Ipv6NaDecoder ipv6NaDecoder = new Ipv6NaDecoder(data);
        NeighborAdvertisePacket naPacket;
        try {
            naPacket = ipv6NaDecoder.decode();
        } catch (UnknownHostException | BufferException e) {
            LOG.warn("Failed to decode IPv6 NA packet={}", data, e);
            return null;
        }
        short tableId = packetReceived.getTableId().getValue();
        LOG.trace("packet: {}, tableId {}, ipv6Type {}", packetReceived, tableId, naPacket.getIcmp6Type());

        BigInteger metadata = packetReceived.getMatch().getMetadata().getMetadata();
        int portTag = MetaDataUtil.getLportFromMetadata(metadata).intValue();
        String interfaceName = null;

        try {
            GetInterfaceFromIfIndexInput input = new GetInterfaceFromIfIndexInputBuilder().setIfIndex(portTag).build();
            Future<RpcResult<GetInterfaceFromIfIndexOutput>> output = interfaceManager.getInterfaceFromIfIndex(input);
            RpcResult<GetInterfaceFromIfIndexOutput> result = output.get();
            if (result.isSuccessful()) {
                GetInterfaceFromIfIndexOutput ifIndexOutput = result.getResult();
                interfaceName = ifIndexOutput.getInterfaceName();
            } else {
                LOG.warn("RPC call to get interface name for if index {} failed with errors {}", portTag,
                        result.getErrors());
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Error retrieving interface Name for tag {}", portTag, e);
        }
        if (StringUtils.isNotBlank(interfaceName)) {
            String sourceIp = Ipv6Util.getFormattedIpv6Address(naPacket.getSourceIpv6());
            String targetIp = Ipv6Util.getFormattedIpv6Address(naPacket.getDestinationIpv6());
            return getMonitoringKey(interfaceName, targetIp, sourceIp);
        } else {
            LOG.debug("No interface associated with tag {} to interpret the received ipv6 NA Reply", portTag);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void startMonitoringTask(MonitoringInfo monitorInfo) {
        EndpointType source = monitorInfo.getSource().getEndpointType();
        final String sourceInterface = Preconditions.checkNotNull(AlivenessMonitorUtil.getInterfaceName(source),
                "Source interface is required to send Ipv6 ND Packet for monitoring");
        final String srcIp = Preconditions.checkNotNull(AlivenessMonitorUtil.getIpAddress(source),
                "Source IP address is required to send Ipv6 ND Packet for monitoring");
        final PhysAddress srcMacAddress = Preconditions.checkNotNull(AlivenessMonitorUtil.getMacAddress(source),
                "Source MAC address is required to send Ipv6 ND Packet for monitoring");

        EndpointType target = monitorInfo.getDestination().getEndpointType();
        final String targetIp = Preconditions.checkNotNull(AlivenessMonitorUtil.getIpAddress(target),
                "Target Ip address is required to send ipv6 ND Packet for monitoring");
        LOG.trace("sendNA interface {}, senderIPAddress {}, targetAddress {}", sourceInterface, srcIp, targetIp);

        SendNeighborSolicitationInput input = buildNsInput(sourceInterface, srcIp, srcMacAddress, targetIp);
        ListenableFuture<RpcResult<SendNeighborSolicitationOutput>> future = ndService.sendNeighborSolicitation(input);

        final String msgFormat =
                String.format("Send NS packet on interface %s to destination %s", sourceInterface, targetIp);
        Futures.addCallback(future, new FutureCallback<RpcResult<SendNeighborSolicitationOutput>>() {
            @Override
            public void onFailure(Throwable error) {
                LOG.error("Error - {}", msgFormat, error);
            }

            @Override
            public void onSuccess(RpcResult<SendNeighborSolicitationOutput> result) {
                if (result != null && !result.isSuccessful()) {
                    LOG.warn("Rpc call to {} failed {}", msgFormat,
                            AlivenessMonitorUtil.getErrorText(result.getErrors()));
                } else {
                    LOG.debug("Successful RPC Result - {}", msgFormat);
                }
            }
        }, MoreExecutors.directExecutor());
    }

    private SendNeighborSolicitationInput buildNsInput(final String sourceInterface, final String srcIp,
            final PhysAddress srcMacAddress, final String targetIp) {
        InterfaceAddressBuilder interfaceAddressBuilder = new InterfaceAddressBuilder().setInterface(sourceInterface)
                .setSrcIpAddress(Ipv6Address.getDefaultInstance(srcIp));
        if (srcMacAddress != null) {
            interfaceAddressBuilder.setSrcMacAddress(srcMacAddress);
        }
        List<InterfaceAddress> addresses = Collections.singletonList(interfaceAddressBuilder.build());
        SendNeighborSolicitationInput input = new SendNeighborSolicitationInputBuilder().setInterfaceAddress(addresses)
                .setTargetIpAddress(Ipv6Address.getDefaultInstance(targetIp)).build();
        return input;
    }

    @Override
    public String getUniqueMonitoringKey(MonitoringInfo monitorInfo) {
        String interfaceName = AlivenessMonitorUtil.getInterfaceName(monitorInfo.getSource().getEndpointType());
        String sourceIp = AlivenessMonitorUtil.getIpAddress(monitorInfo.getSource().getEndpointType());
        String targetIp = AlivenessMonitorUtil.getIpAddress(monitorInfo.getDestination().getEndpointType());
        return getMonitoringKey(interfaceName, sourceIp, targetIp);
    }

    private String getMonitoringKey(String interfaceName, String sourceIp, String targetIp) {
        return interfaceName + SEPERATOR + sourceIp + SEPERATOR + targetIp + SEPERATOR + EtherTypes.Ipv6Nd;
    }
}

