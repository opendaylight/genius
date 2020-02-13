/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.alivenessmonitor.protocols.internal;

import static org.opendaylight.genius.alivenessmonitor.protocols.AlivenessMonitorAndProtocolsConstants.SEPERATOR;

import java.util.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.opendaylight.genius.alivenessmonitor.protocols.AlivenessProtocolHandlerRegistry;
import org.opendaylight.genius.alivenessmonitor.utils.AlivenessMonitorUtil;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NWUtil;
import org.opendaylight.genius.mdsalutil.packet.ARP;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.MonitorProtocolType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.EndpointType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.endpoint.endpoint.type.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.rev160411.monitor.configs.MonitoringInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpRequestInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpRequestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.interfaces.InterfaceAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.interfaces.InterfaceAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AlivenessProtocolHandlerARP extends AbstractAlivenessProtocolHandler<ARP> {

    private static final Logger LOG = LoggerFactory.getLogger(AlivenessProtocolHandlerARP.class);

    private final OdlArputilService arpService;
    private final OdlInterfaceRpcService interfaceManager;

    @Inject
    public AlivenessProtocolHandlerARP(
            @Reference final DataBroker dataBroker,
            @Reference final AlivenessProtocolHandlerRegistry alivenessProtocolHandlerRegistry,
            final OdlInterfaceRpcService interfaceManager,
            final OdlArputilService arpService) {
        super(dataBroker, alivenessProtocolHandlerRegistry, MonitorProtocolType.Arp);
        this.interfaceManager = interfaceManager;
        this.arpService = arpService;
    }

    @Override
    public Class<ARP> getPacketClass() {
        return ARP.class;
    }

    @Override
    @SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
    public String handlePacketIn(ARP packet, PacketReceived packetReceived) {
        short tableId = packetReceived.getTableId().getValue().toJava();
        int arpType = packet.getOpCode();

        if (LOG.isTraceEnabled()) {
            LOG.trace("packet: {}, tableId {}, arpType {}", packetReceived, tableId, arpType);
        }

        if (arpType == ARP.REPLY) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("packet: {}", packetReceived);
            }

            Uint64 metadata = packetReceived.getMatch().getMetadata().getMetadata();
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
                String sourceIp = NWUtil.toStringIpAddress(packet.getSenderProtocolAddress());
                String targetIp = NWUtil.toStringIpAddress(packet.getTargetProtocolAddress());
                return getMonitoringKey(interfaceName, targetIp, sourceIp);
            } else {
                LOG.debug("No interface associated with tag {} to interpret the received ARP Reply", portTag);
            }
        }

        return null;
    }

    @Override
    public void startMonitoringTask(MonitoringInfo monitorInfo) {
        if (arpService == null) {
            LOG.debug("ARP Service not available to send the packet");
            return;
        }
        EndpointType source = monitorInfo.getSource().getEndpointType();
        final String sourceInterface = Preconditions.checkNotNull(AlivenessMonitorUtil.getInterfaceName(source),
                "Source interface is required to send ARP Packet for monitoring");

        final String srcIp = Preconditions.checkNotNull(AlivenessMonitorUtil.getIpAddress(source),
                "Source Ip address is required to send ARP Packet for monitoring");
        final Optional<PhysAddress> srcMacAddressOptional = getMacAddress(source);
        if (srcMacAddressOptional.isPresent()) {
            PhysAddress srcMacAddress = srcMacAddressOptional.get();
            EndpointType target = monitorInfo.getDestination().getEndpointType();
            final String targetIp = Preconditions.checkNotNull(AlivenessMonitorUtil.getIpAddress(target),
                    "Target Ip address is required to send ARP Packet for monitoring");
            if (LOG.isTraceEnabled()) {
                LOG.trace("sendArpRequest interface {}, senderIPAddress {}, targetAddress {}", sourceInterface, srcIp,
                        targetIp);
            }
            InterfaceAddressBuilder interfaceAddressBuilder = new InterfaceAddressBuilder()
                    .setInterface(sourceInterface).setIpAddress(IpAddressBuilder.getDefaultInstance(srcIp));
            if (srcMacAddress != null) {
                interfaceAddressBuilder.setMacaddress(srcMacAddress);
            }
            List<InterfaceAddress> addresses = Collections.singletonList(interfaceAddressBuilder.build());
            SendArpRequestInput input = new SendArpRequestInputBuilder().setInterfaceAddress(addresses)
                    .setIpaddress(IpAddressBuilder.getDefaultInstance(targetIp)).build();
            ListenableFuture<RpcResult<SendArpRequestOutput>> future = arpService.sendArpRequest(input);
            final String msgFormat = String.format("Send ARP Request on interface %s to destination %s",
                    sourceInterface, targetIp);
            Futures.addCallback(future, new FutureCallback<RpcResult<SendArpRequestOutput>>() {
                @Override
                public void onFailure(Throwable error) {
                    LOG.error("Error - {}", msgFormat, error);
                }

                @Override
                public void onSuccess(RpcResult<SendArpRequestOutput> result) {
                    if (result != null && !result.isSuccessful()) {
                        LOG.warn("Rpc call to {} failed {}", msgFormat,
                                AlivenessMonitorUtil.getErrorText(result.getErrors()));
                    } else {
                        LOG.debug("Successful RPC Result - {}", msgFormat);
                    }
                }
            }, MoreExecutors.directExecutor());
        }
    }

    @Override
    public String getUniqueMonitoringKey(MonitoringInfo monitorInfo) {
        String interfaceName = AlivenessMonitorUtil.getInterfaceName(monitorInfo.getSource().getEndpointType());
        String sourceIp = AlivenessMonitorUtil.getIpAddress(monitorInfo.getSource().getEndpointType());
        String targetIp = AlivenessMonitorUtil.getIpAddress(monitorInfo.getDestination().getEndpointType());
        return getMonitoringKey(interfaceName, sourceIp, targetIp);
    }

    private String getMonitoringKey(String interfaceName, String sourceIp, String targetIp) {
        return interfaceName + SEPERATOR + sourceIp + SEPERATOR + targetIp + SEPERATOR + MonitorProtocolType.Arp;
    }

    private Optional<PhysAddress> getMacAddress(EndpointType source) {
        Optional<PhysAddress> result = Optional.empty();
        if (source instanceof Interface) {
            result = Optional.of(((Interface) source).getMacAddress());
        }
        return result;
    }
}
