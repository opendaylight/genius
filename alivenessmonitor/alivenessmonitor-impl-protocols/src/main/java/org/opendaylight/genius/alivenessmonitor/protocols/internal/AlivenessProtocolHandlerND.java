/*
 * Copyright (c) 2018 Alten Calsoft Labs India Pvt Ltd and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


package org.opendaylight.genius.alivenessmonitor.protocols.internal;
//package org.opendaylight.genius.ipv6util.nd;

import static org.opendaylight.genius.alivenessmonitor.protocols.AlivenessMonitorAndProtocolsConstants.SEPERATOR;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NWUtil;
import org.opendaylight.genius.mdsalutil.packet.ND;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.EtherTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.EndpointType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
//ported NITHI
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.Ipv6NdUtilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.SendNeighborSolicitationInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.SendNeighborSolicitationInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.SendNeighborSolicitationOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.interfaces.InterfaceAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.ipv6.nd.util.rev170210.interfaces.InterfaceAddressBuilder;

//ported NITHI
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlivenessProtocolHandlerND extends AbstractAlivenessProtocolHandler<ND> {

    private static final Logger LOG = LoggerFactory.getLogger(AlivenessProtocolHandlerND.class);
    public static final long FAILURE_THRESHOLD = 2L;
    public static final long MONITORING_WINDOW = 4L;

    private final Ipv6NdUtilService ndService;
    private final OdlInterfaceRpcService interfaceManager;

    public AlivenessProtocolHandlerND(
            final DataBroker dataBroker,
            final OdlInterfaceRpcService interfaceManager,
            final AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry,
            final Ipv6NdUtilService ndService) {
        super(dataBroker, alivenessProtocolHandlerRegistry, EtherTypes.Nd);
        this.interfaceManager = interfaceManager;
        this.ndService = ndService;
    }

    @Override
    public Class<ND> getPacketClass() {
        return ND.class;
    }
    @Override
    @SuppressFBWarnings(value = "NP_NULL_PARAM_DEREF", justification = "Unrecognised NullableDecl")
    public String handlePacketIn(ND packet, PacketReceived packetReceived) {
        short tableId = packetReceived.getTableId().getValue();
        byte ipv6Type = packet.getIcmp6Type();

        if (LOG.isTraceEnabled()) {
            LOG.trace("packet: {}, tableId {}, ipv6Type {}", packetReceived, tableId, ipv6Type);
        }

        if (ipv6Type == (byte)ND.NEIGHBOR_ADVERTISEMENT) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("packet: {}", packetReceived);
            }

            BigInteger metadata = packetReceived.getMatch().getMetadata().getMetadata();
            int portTag = MetaDataUtil.getLportFromMetadata(metadata).intValue();
            String interfaceName = null;

            try {
                GetInterfaceFromIfIndexInput input = new GetInterfaceFromIfIndexInputBuilder().setIfIndex(portTag)
                        .build();
                Future<RpcResult<GetInterfaceFromIfIndexOutput>> output = interfaceManager
                        .getInterfaceFromIfIndex(input);
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
            if (!Strings.isNullOrEmpty(interfaceName)) {
                String sourceIp = NWUtil.toStringIpAddress(packet.getSrcIpAddress());
                String targetIp = NWUtil.toStringIpAddress(packet.getTargetAddress());
                return getMonitoringKey(interfaceName, targetIp, sourceIp);
            } else {
                LOG.debug("No interface associated with tag {} to interpret the received ipv6 NA Reply", portTag);
            }
        }

        return null;
    }

    @Override
    public void startMonitoringTask(MonitoringInfo monitorInfo) {
        if (ndService == null) {
            LOG.debug("ipv6 ND Service not available to send the packet");
            return;
        }
        EndpointType source = monitorInfo.getSource().getEndpointType();
        final String sourceInterface = Preconditions.checkNotNull(getInterfaceName(source),
                "Source interface is required to send ipv6 ND Packet for monitoring");

        final String srcIp = Preconditions.checkNotNull(getIpAddress(source),
                "Source Ip address is required to send ipv6 ND Packet for monitoring");
        final Optional<PhysAddress> srcMacAddressOptional = getMacAddress(source);
        if (srcMacAddressOptional.isPresent()) {
            PhysAddress srcMacAddress = srcMacAddressOptional.get();
            EndpointType target = monitorInfo.getDestination().getEndpointType();
            final String targetIp = Preconditions.checkNotNull(getIpAddress(target),
                    "Target Ip address is required to send ipv6 ND Packet for monitoring");
            if (LOG.isTraceEnabled()) {
                LOG.trace("sendNA interface {}, senderIPAddress {}, targetAddress {}", sourceInterface, srcIp,
                        targetIp);
            }
            InterfaceAddressBuilder interfaceAddressBuilder = new InterfaceAddressBuilder()
                    .setInterface(sourceInterface).setSrcIpAddress(Ipv6Address.getDefaultInstance(srcIp));
            if (srcMacAddress != null) {
                interfaceAddressBuilder.setSrcMacAddress(srcMacAddress);
            }
            List<InterfaceAddress> addresses = Collections.singletonList(interfaceAddressBuilder.build());
            SendNeighborSolicitationInput input = new SendNeighborSolicitationInputBuilder().setInterfaceAddress(addresses)
                    .setTargetIpAddress(Ipv6Address.getDefaultInstance(targetIp)).build();
            ListenableFuture<RpcResult<SendNeighborSolicitationOutput>> future = ndService.sendNeighborSolicitation(input);
            final String msgFormat = String.format("Send NS packet on interface %s to destination %s",
                    sourceInterface, targetIp);
            Futures.addCallback(JdkFutureAdapters.listenInPoolThread(future), new FutureCallback<RpcResult<SendNeighborSolicitationOutput>>() {
                @Override
                public void onFailure(Throwable error) {
                    LOG.error("Error - {}", msgFormat, error);
                }

                @Override
                public void onSuccess(RpcResult<SendNeighborSolicitationOutput> result) {
                    if (result != null && !result.isSuccessful()) {
                        LOG.warn("Rpc call to {} failed {}", msgFormat, getErrorText(result.getErrors()));
                    } else {
                        LOG.debug("Successful RPC Result - {}", msgFormat);
                    }
                }
            });
        }
    }
    private String getErrorText(Collection<RpcError> errors) {
        StringBuilder errorText = new StringBuilder();
        for (RpcError error : errors) {
            errorText.append(",").append(error.getErrorType()).append("-").append(error.getMessage());
        }
        return errorText.toString();
    }

    @Override
    public String getUniqueMonitoringKey(MonitoringInfo monitorInfo) {
        String interfaceName = getInterfaceName(monitorInfo.getSource().getEndpointType());
        String sourceIp = getIpAddress(monitorInfo.getSource().getEndpointType());
        String targetIp = getIpAddress(monitorInfo.getDestination().getEndpointType());
        return getMonitoringKey(interfaceName, sourceIp, targetIp);
    }

    private String getMonitoringKey(String interfaceName, String sourceIp, String targetIp) {
        return interfaceName + SEPERATOR + sourceIp + SEPERATOR + targetIp + SEPERATOR + EtherTypes.Nd;
    }

    private String getIpAddress(EndpointType source) {
        String ipAddress = null;
        if (source instanceof IpAddress) {
            ipAddress = ((IpAddress) source).getIpAddress().getIpv6Address().getValue();
        } else if (source instanceof Interface) {
            ipAddress = ((Interface) source).getInterfaceIp().getIpv6Address().getValue();
        }
        return ipAddress;
    }

    private Optional<PhysAddress> getMacAddress(EndpointType source) {
        Optional<PhysAddress> result = Optional.absent();
        if (source instanceof Interface) {
            result = Optional.of(((Interface) source).getMacAddress());
        }
        return result;
    }

    private String getInterfaceName(EndpointType endpoint) {
        String interfaceName = null;
        if (endpoint instanceof Interface) {
            interfaceName = ((Interface) endpoint).getInterfaceName();
        }
        return interfaceName;
    }
}

