/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.arputil.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.arputil.api.ArpConstants;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MetaDataUtil;
import org.opendaylight.genius.mdsalutil.NWUtil;
import org.opendaylight.genius.mdsalutil.packet.ARP;
import org.opendaylight.genius.mdsalutil.packet.Ethernet;
import org.opendaylight.infrautils.inject.AbstractLifecycle;
import org.opendaylight.infrautils.metrics.Meter;
import org.opendaylight.infrautils.metrics.MetricProvider;
import org.opendaylight.openflowplugin.libraries.liblldp.HexEncode;
import org.opendaylight.openflowplugin.libraries.liblldp.Packet;
import org.opendaylight.openflowplugin.libraries.liblldp.PacketException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.PhysAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.ArpRequestReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.ArpResponseReceivedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.GetMacInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.GetMacOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.GetMacOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.MacChangedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.OdlArputilService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpRequestInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpRequestInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpRequestOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpResponseInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.SendArpResponseOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.rev160406.interfaces.InterfaceAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetEgressActionsForInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetInterfaceFromIfIndexOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.GetPortFromInterfaceOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rpcs.rev160406.OdlInterfaceRpcService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Metadata;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketInReason;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.SendToController;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketOutput;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ArpUtilImpl extends AbstractLifecycle implements OdlArputilService, PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(ArpUtilImpl.class);
    private static final String MODULENAME = "odl.genius.arputil.";
    private static final String OPENFLOW_PFX = "openflow:";

    private final DataBroker dataBroker;
    private final PacketProcessingService packetProcessingService;
    private final NotificationPublishService notificationPublishService;
    private final NotificationService notificationService;
    private final OdlInterfaceRpcService odlInterfaceRpcService;
    private ListenerRegistration<ArpUtilImpl> listenerRegistration;
    private final ExecutorService threadPool = Executors.newFixedThreadPool(1);
    private final ConcurrentMap<String, String> macsDB = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SettableFuture<RpcResult<GetMacOutput>>> macAddrs = new ConcurrentHashMap<>();

    private final Meter arpRespRecvd;
    private final Meter arpRespRecvdNotification;
    private final Meter arpRespRecvdNotificationRejected;
    private final Meter arpReqRecvd;
    private final Meter arpReqRecvdNotification;
    private final Meter arpReqRecvdNotificationRejected;


    @Inject
    public ArpUtilImpl(final DataBroker dataBroker, final PacketProcessingService packetProcessingService,
                       final NotificationPublishService notificationPublishService,
                       final NotificationService notificationService,
                       final OdlInterfaceRpcService odlInterfaceRpcService,
                       final MetricProvider metricProvider) {
        this.dataBroker = dataBroker;
        this.packetProcessingService = packetProcessingService;
        this.notificationPublishService = notificationPublishService;
        this.notificationService = notificationService;
        this.odlInterfaceRpcService = odlInterfaceRpcService;

        arpRespRecvd = metricProvider.newMeter(this,MODULENAME + "arpResponseReceived");
        arpRespRecvdNotification = metricProvider.newMeter(this,MODULENAME + "arpResponseReceivedNotification");
        arpRespRecvdNotificationRejected = metricProvider.newMeter(this,
                MODULENAME + "arpResponseReceivedNotificationRejected");
        arpReqRecvd = metricProvider.newMeter(this,MODULENAME + "arpRequestReceived");
        arpReqRecvdNotification = metricProvider.newMeter(this,MODULENAME + "arpRequestReceivedNotification");
        arpReqRecvdNotificationRejected = metricProvider.newMeter(this,
                MODULENAME + "arpRequestReceivedNotificationRejected");
    }

    @Override
    public void start() {
        LOG.info("{} start", getClass().getSimpleName());
        listenerRegistration = notificationService.registerNotificationListener(this);
    }

    @Override
    public void stop() {
        LOG.info("{} stop", getClass().getSimpleName());

        if (listenerRegistration != null) {
            listenerRegistration.close();
            listenerRegistration = null;
        }
    }

    private String getIpAddressInString(IpAddress ipAddress) throws UnknownHostException {
        return InetAddress.getByName(ipAddress.getIpv4Address().getValue()).getHostAddress();
    }

    @Override
    public ListenableFuture<RpcResult<GetMacOutput>> getMac(GetMacInput input) {
        try {
            final String dstIpAddress = getIpAddressInString(input.getIpaddress());
            LOG.trace("getMac rpc invoked for ip {}", dstIpAddress);
            if (macAddrs.get(dstIpAddress) != null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("get mac already in progress for the ip {}", dstIpAddress);
                }
                return macAddrs.get(dstIpAddress);
            }
            SendArpRequestInputBuilder builder = new SendArpRequestInputBuilder()
                    .setInterfaceAddress(input.getInterfaceAddress()).setIpaddress(input.getIpaddress());
            ListenableFuture<RpcResult<SendArpRequestOutput>> arpReqFt = sendArpRequest(builder.build());
            final SettableFuture<RpcResult<GetMacOutput>> ft = SettableFuture.create();

            Futures.addCallback(arpReqFt, new FutureCallback<RpcResult<SendArpRequestOutput>>() {
                @Override
                public void onFailure(Throwable ex) {
                    RpcResultBuilder<GetMacOutput> resultBuilder = RpcResultBuilder.<GetMacOutput>failed()
                            .withError(ErrorType.APPLICATION, ex.getMessage(), ex);
                    ft.set(resultBuilder.build());
                }

                @Override
                public void onSuccess(RpcResult<SendArpRequestOutput> result) {
                    LOG.trace("Successfully sent the arp pkt out for ip {}", dstIpAddress);
                }
            }, MoreExecutors.directExecutor());

            macAddrs.put(dstIpAddress, ft);
            return ft;
        } catch (UnknownHostException e) {
            LOG.error("Failed to handle getMac request for {}", input.getIpaddress(), e);
            RpcResultBuilder<GetMacOutput> resultBuilder = RpcResultBuilder.<GetMacOutput>failed()
                    .withError(ErrorType.APPLICATION, e.getMessage(), e);
            return Futures.immediateFuture(resultBuilder.build());
        }
    }

    private byte[] getIpAddressBytes(IpAddress ip) throws UnknownHostException {
        return InetAddress.getByName(ip.getIpv4Address().getValue()).getAddress();
    }

    @Override
    public ListenableFuture<RpcResult<SendArpRequestOutput>> sendArpRequest(SendArpRequestInput arpReqInput) {
        LOG.trace("rpc sendArpRequest invoked for ip {}", arpReqInput.getIpaddress());
        BigInteger dpnId;
        byte[] payload;
        String interfaceName = null;
        byte[] srcIpBytes;
        byte[] dstIpBytes;
        byte[] srcMac;

        RpcResultBuilder<SendArpRequestOutput> failureBuilder = RpcResultBuilder.failed();
        RpcResultBuilder<SendArpRequestOutput> successBuilder = RpcResultBuilder.success();

        try {
            dstIpBytes = getIpAddressBytes(arpReqInput.getIpaddress());
        } catch (UnknownHostException e) {
            LOG.error("Cannot get IP address", e);
            failureBuilder.withError(ErrorType.APPLICATION, ArpConstants.UNKNOWN_IP_ADDRESS_SUPPLIED);
            return Futures.immediateFuture(failureBuilder.build());
        }

        int localErrorCount = 0;
        for (InterfaceAddress interfaceAddress : arpReqInput.getInterfaceAddress()) {
            try {
                interfaceName = interfaceAddress.getInterface();
                srcIpBytes = getIpAddressBytes(interfaceAddress.getIpAddress());

                GetPortFromInterfaceOutput portResult = getPortFromInterface(interfaceName);
                checkNotNull(portResult);
                dpnId = portResult.getDpid();
                Long portid = portResult.getPortno();
                checkArgument(null != dpnId && !BigInteger.ZERO.equals(dpnId),
                    ArpConstants.DPN_NOT_FOUND_ERROR, interfaceName);

                NodeConnectorRef ref = MDSALUtil.getNodeConnRef(dpnId, portid.toString());
                checkNotNull(ref, ArpConstants.NODE_CONNECTOR_NOT_FOUND_ERROR, interfaceName);

                LOG.trace("sendArpRequest received dpnId {} out interface {}", dpnId, interfaceName);
                if (interfaceAddress.getMacaddress() == null) {
                    srcMac = MDSALUtil.getMacAddressForNodeConnector(dataBroker,
                            (InstanceIdentifier<NodeConnector>) ref.getValue());
                } else {
                    String macAddr = interfaceAddress.getMacaddress().getValue();
                    srcMac = HexEncode.bytesFromHexString(macAddr);
                }
                checkNotNull(srcMac, ArpConstants.FAILED_TO_GET_SRC_MAC_FOR_INTERFACE, interfaceName, ref.getValue());
                checkNotNull(srcIpBytes, ArpConstants.FAILED_TO_GET_SRC_IP_FOR_INTERFACE, interfaceName);

                payload = ArpPacketUtil.getPayload(ArpConstants.ARP_REQUEST_OP, srcMac, srcIpBytes,
                        ArpPacketUtil.ETHERNET_BROADCAST_DESTINATION, dstIpBytes);

                List<Action> actions = getEgressAction(interfaceName);
                sendPacketOutWithActions(dpnId, payload, ref, actions);

                LOG.trace("sent arp request for {}", arpReqInput.getIpaddress());
            } catch (UnknownHostException | PacketException | InterruptedException | ExecutionException
                    | ReadFailedException e) {
                LOG.trace("failed to send arp req for {} on interface {}", arpReqInput.getIpaddress(), interfaceName);

                failureBuilder.withError(ErrorType.APPLICATION,
                    ArpConstants.FAILED_TO_SEND_ARP_REQ_FOR_INTERFACE + interfaceName, e);
                successBuilder.withError(ErrorType.APPLICATION,
                    ArpConstants.FAILED_TO_SEND_ARP_REQ_FOR_INTERFACE + interfaceName, e);
                localErrorCount++;
            }
        }
        if (localErrorCount == arpReqInput.getInterfaceAddress().size()) {
            // All the requests failed
            return Futures.immediateFuture(failureBuilder.build());
        }
        return Futures.immediateFuture(successBuilder.build());
    }

    public ListenableFuture<RpcResult<TransmitPacketOutput>> sendPacketOut(
            BigInteger dpnId, byte[] payload, NodeConnectorRef ref) {
        NodeConnectorRef nodeConnectorRef = MDSALUtil.getNodeConnRef(dpnId, "0xfffffffd");
        return packetProcessingService.transmitPacket(new TransmitPacketInputBuilder().setPayload(payload)
                .setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, new NodeKey(new NodeId(OPENFLOW_PFX + dpnId))).build()))
                .setIngress(nodeConnectorRef).setEgress(ref).build());
    }

    private Future<RpcResult<TransmitPacketOutput>> sendPacketOutWithActions(
            BigInteger dpnId, byte[] payload, NodeConnectorRef ref, List<Action> actions) {
        NodeConnectorRef nodeConnectorRef = MDSALUtil.getNodeConnRef(dpnId, "0xfffffffd");
        TransmitPacketInput transmitPacketInput = new TransmitPacketInputBuilder().setPayload(payload)
                .setNode(new NodeRef(InstanceIdentifier.builder(Nodes.class)
                        .child(Node.class, new NodeKey(new NodeId(OPENFLOW_PFX + dpnId))).build()))
                .setIngress(nodeConnectorRef).setEgress(ref).setAction(actions).build();
        LOG.trace("PacketOut message framed for transmitting {}", transmitPacketInput);
        return packetProcessingService.transmitPacket(transmitPacketInput);
    }

    private List<Action> getEgressAction(String interfaceName) {
        List<Action> actions = new ArrayList<>();
        try {
            GetEgressActionsForInterfaceInputBuilder egressAction = new GetEgressActionsForInterfaceInputBuilder()
                    .setIntfName(interfaceName);
            OdlInterfaceRpcService intfRpc = odlInterfaceRpcService;
            if (intfRpc == null) {
                LOG.error("Unable to obtain interfaceMgrRpc service, ignoring egress actions for interfaceName {}",
                        interfaceName);
                return actions;
            }
            Future<RpcResult<GetEgressActionsForInterfaceOutput>> result = intfRpc
                    .getEgressActionsForInterface(egressAction.build());
            RpcResult<GetEgressActionsForInterfaceOutput> rpcResult = result.get();
            if (!rpcResult.isSuccessful()) {
                LOG.warn("RPC Call to Get egress actions for interface {} returned with Errors {}", interfaceName,
                        rpcResult.getErrors());
            } else {
                actions = rpcResult.getResult().getAction();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Exception when egress actions for interface {}", interfaceName, e);
        }
        return actions;
    }

    @Override
    public ListenableFuture<RpcResult<SendArpResponseOutput>> sendArpResponse(SendArpResponseInput input) {
        LOG.trace("sendArpResponse rpc invoked");
        BigInteger dpnId;
        byte[] payload;
        byte[] srcMac;

        try {
            String interfaceName = input.getInterface();
            GetPortFromInterfaceOutput portResult = getPortFromInterface(interfaceName);
            checkNotNull(portResult);
            dpnId = portResult.getDpid();
            Long portid = portResult.getPortno();
            NodeConnectorRef ref = MDSALUtil.getNodeConnRef(dpnId, portid.toString());
            checkArgument(null != dpnId && !BigInteger.ZERO.equals(dpnId),
                ArpConstants.DPN_NOT_FOUND_ERROR, interfaceName);
            checkNotNull(ref, ArpConstants.NODE_CONNECTOR_NOT_FOUND_ERROR, interfaceName);

            LOG.trace("sendArpRequest received dpnId {} out interface {}", dpnId, interfaceName);

            byte[] srcIpBytes = getIpAddressBytes(input.getSrcIpaddress());
            byte[] dstIpBytes = getIpAddressBytes(input.getDstIpaddress());
            if (input.getSrcMacaddress() == null) {
                srcMac = portResult.getPhyAddress().getBytes("UTF-8");
            } else {
                String macAddr = input.getSrcMacaddress().getValue();
                srcMac = HexEncode.bytesFromHexString(macAddr);
            }
            byte[] dstMac = NWUtil.parseMacAddress(input.getDstMacaddress().getValue());
            checkNotNull(srcIpBytes, ArpConstants.FAILED_TO_GET_SRC_IP_FOR_INTERFACE, interfaceName);
            payload = ArpPacketUtil.getPayload(ArpConstants.ARP_RESPONSE_OP, srcMac, srcIpBytes, dstMac, dstIpBytes);

            List<Action> actions = getEgressAction(interfaceName);
            sendPacketOutWithActions(dpnId, payload, ref, actions);
            LOG.debug("Sent ARP response for IP {}, from source MAC {} to target MAC {} and target IP {} via dpnId {}",
                    input.getSrcIpaddress().getIpv4Address().getValue(), HexEncode.bytesToHexStringFormat(srcMac),
                    HexEncode.bytesToHexStringFormat(dstMac), input.getDstIpaddress().getIpv4Address().getValue(),
                    dpnId);
        } catch (UnknownHostException | PacketException | InterruptedException | UnsupportedEncodingException
                | ExecutionException e) {
            LOG.error("failed to send arp response for {}: ", input.getSrcIpaddress(), e);
            return RpcResultBuilder.<SendArpResponseOutput>failed()
                    .withError(ErrorType.APPLICATION, e.getMessage(), e).buildFuture();
        }
        RpcResultBuilder<SendArpResponseOutput> rpcResultBuilder = RpcResultBuilder.success();
        return Futures.immediateFuture(rpcResultBuilder.build());
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        Class<? extends PacketInReason> pktInReason = packetReceived.getPacketInReason();
        LOG.trace("Packet Received {}", packetReceived);

        if (pktInReason == SendToController.class) {
            try {
                BigInteger dpnId = extractDpnId(packetReceived);
                int tableId = packetReceived.getTableId().getValue();

                byte[] data = packetReceived.getPayload();
                Ethernet ethernet = new Ethernet();

                ethernet.deserialize(data, 0, data.length * Byte.SIZE);
                if (ethernet.getEtherType() != ArpConstants.ETH_TYPE_ARP) {
                    return;
                }

                Packet pkt = ethernet.getPayload();
                ARP arp = (ARP) pkt;
                InetAddress srcInetAddr = InetAddress.getByAddress(arp.getSenderProtocolAddress());
                InetAddress dstInetAddr = InetAddress.getByAddress(arp.getTargetProtocolAddress());
                byte[] srcMac = ethernet.getSourceMACAddress();
                byte[] dstMac = ethernet.getDestinationMACAddress();

                Metadata metadata = packetReceived.getMatch().getMetadata();

                String interfaceName = getInterfaceName(metadata);

                checkAndFireMacChangedNotification(interfaceName, srcInetAddr, srcMac);
                macsDB.put(interfaceName + "-" + srcInetAddr.getHostAddress(), NWUtil.toStringMacAddress(srcMac));
                if (arp.getOpCode() == ArpConstants.ARP_REQUEST_OP) {
                    fireArpReqRecvdNotification(interfaceName, srcInetAddr, srcMac, dstInetAddr, dpnId, tableId,
                            metadata.getMetadata());
                } else {
                    fireArpRespRecvdNotification(interfaceName, srcInetAddr, srcMac, dpnId, tableId,
                                                 metadata.getMetadata(), dstInetAddr, dstMac);
                }
                if (macAddrs.get(srcInetAddr.getHostAddress()) != null) {
                    threadPool.execute(new MacResponderTask(arp));
                }
            } catch (PacketException | UnknownHostException | InterruptedException | ExecutionException e) {
                LOG.trace("Failed to decode packet", e);
            }
        }
    }

    private GetPortFromInterfaceOutput getPortFromInterface(String interfaceName)
            throws InterruptedException, ExecutionException {
        GetPortFromInterfaceInputBuilder getPortFromInterfaceInputBuilder = new GetPortFromInterfaceInputBuilder();
        getPortFromInterfaceInputBuilder.setIntfName(interfaceName);

        Future<RpcResult<GetPortFromInterfaceOutput>> portFromInterface = odlInterfaceRpcService
                .getPortFromInterface(getPortFromInterfaceInputBuilder.build());
        GetPortFromInterfaceOutput result = portFromInterface.get().getResult();
        LOG.trace("getPortFromInterface rpc result is {} ", result);
        if (result != null) {
            LOG.trace("getPortFromInterface rpc result is {} {} ", result.getDpid(), result.getPortno());
        }
        return result;
    }

    private String getInterfaceName(Metadata metadata)
            throws InterruptedException, ExecutionException {
        LOG.debug("metadata received is {} ", metadata);

        GetInterfaceFromIfIndexInputBuilder ifIndexInputBuilder = new GetInterfaceFromIfIndexInputBuilder();
        BigInteger lportTag = MetaDataUtil.getLportFromMetadata(metadata.getMetadata());

        ifIndexInputBuilder.setIfIndex(lportTag.intValue());
        GetInterfaceFromIfIndexInput input = ifIndexInputBuilder.build();

        Future<RpcResult<GetInterfaceFromIfIndexOutput>> interfaceFromIfIndex = odlInterfaceRpcService
                .getInterfaceFromIfIndex(input);
        GetInterfaceFromIfIndexOutput interfaceFromIfIndexOutput = interfaceFromIfIndex.get().getResult();
        return interfaceFromIfIndexOutput.getInterfaceName();
    }

    private class MacResponderTask implements Runnable {
        final ARP arp;

        MacResponderTask(ARP arp) {
            this.arp = arp;
        }

        @Override
        public void run() {
            InetAddress srcAddr;
            GetMacOutputBuilder outputBuilder;
            String srcMac;
            try {
                srcAddr = InetAddress.getByAddress(arp.getSenderProtocolAddress());
                srcMac = NWUtil.toStringMacAddress(arp.getSenderHardwareAddress());
                SettableFuture<RpcResult<GetMacOutput>> future = macAddrs.remove(srcAddr.getHostAddress());
                if (future == null) {
                    LOG.trace("There are no pending mac requests.");
                    return;
                }
                outputBuilder = new GetMacOutputBuilder().setMacaddress(new PhysAddress(srcMac));
                future.set(RpcResultBuilder.success(outputBuilder.build()).build());
                if (LOG.isTraceEnabled()) {
                    LOG.trace("sent the mac response for ip {}", srcAddr.getHostAddress());
                }
            } catch (UnknownHostException e) {
                LOG.error("failed to send mac response", e);
            }
        }
    }

    private void fireArpRespRecvdNotification(String interfaceName, InetAddress srcInetAddr, byte[] srcMacAddressBytes,
            BigInteger dpnId, int tableId, BigInteger metadata, InetAddress dstInetAddr, byte[] dstMacAddressBytes)
                    throws InterruptedException {
        arpRespRecvd.mark();

        IpAddress srcIp = IetfInetUtil.INSTANCE.ipAddressFor(srcInetAddr);
        IpAddress dstIp = IetfInetUtil.INSTANCE.ipAddressFor(dstInetAddr);
        String srcMacAddress = NWUtil.toStringMacAddress(srcMacAddressBytes);
        PhysAddress srcMac = new PhysAddress(srcMacAddress);
        String dstMacAddress = NWUtil.toStringMacAddress(dstMacAddressBytes);
        PhysAddress dstMac = new PhysAddress(dstMacAddress);
        ArpResponseReceivedBuilder builder = new ArpResponseReceivedBuilder();
        builder.setInterface(interfaceName);
        builder.setSrcIpaddress(srcIp);
        builder.setDpnId(dpnId);
        builder.setOfTableId((long) tableId);
        builder.setSrcMac(srcMac);
        builder.setMetadata(metadata);
        builder.setDstIpaddress(dstIp);
        builder.setDstMac(dstMac);
        ListenableFuture<?> offerNotification = notificationPublishService.offerNotification(builder.build());
        if (offerNotification != null && offerNotification.equals(NotificationPublishService.REJECTED)) {
            arpRespRecvdNotificationRejected.mark();

        } else {
            arpRespRecvdNotification.mark();
        }
    }

    private void fireArpReqRecvdNotification(String interfaceName, InetAddress srcInetAddr, byte[] srcMac,
            InetAddress dstInetAddr, BigInteger dpnId, int tableId, BigInteger metadata) throws InterruptedException {
        arpReqRecvd.mark();
        String macAddress = NWUtil.toStringMacAddress(srcMac);
        ArpRequestReceivedBuilder builder = new ArpRequestReceivedBuilder();
        builder.setInterface(interfaceName);
        builder.setDpnId(dpnId);
        builder.setOfTableId((long) tableId);
        builder.setSrcIpaddress(IetfInetUtil.INSTANCE.ipAddressFor(srcInetAddr));
        builder.setDstIpaddress(IetfInetUtil.INSTANCE.ipAddressFor(dstInetAddr));
        builder.setSrcMac(new PhysAddress(macAddress));
        builder.setMetadata(metadata);
        ListenableFuture<?> offerNotification = notificationPublishService.offerNotification(builder.build());
        if (offerNotification != null && offerNotification.equals(NotificationPublishService.REJECTED)) {
            arpReqRecvdNotificationRejected.mark();
        } else {
            arpReqRecvdNotification.mark();
        }
    }

    private void checkAndFireMacChangedNotification(String interfaceName, InetAddress inetAddr, byte[] macAddressBytes)
            throws InterruptedException {

        IpAddress ip = IetfInetUtil.INSTANCE.ipAddressFor(inetAddr);
        String macAddress = NWUtil.toStringMacAddress(macAddressBytes);
        PhysAddress mac = new PhysAddress(macAddress);

        if (!macAddress.equals(macsDB.get(interfaceName + "-" + inetAddr.getHostAddress()))) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("mac address changed for {}", inetAddr);
            }
            MacChangedBuilder builder = new MacChangedBuilder();
            builder.setInterface(interfaceName);
            builder.setIpaddress(ip);
            builder.setMacaddress(mac);
            notificationPublishService.putNotification(builder.build());
        }
    }

    private BigInteger extractDpnId(PacketReceived packetReceived) {
        NodeKey nodeKey = packetReceived.getIngress().getValue().firstKeyOf(Node.class);
        String nodeKeyString = nodeKey.getId().getValue();

        if (!nodeKeyString.startsWith(OPENFLOW_PFX)) {
            LOG.warn("Could not extract DPN for packet-in, doesn't start with 'openflow:' {}", packetReceived);
            return null;
        }

        return new BigInteger(nodeKeyString.substring(OPENFLOW_PFX.length()));
    }
}
