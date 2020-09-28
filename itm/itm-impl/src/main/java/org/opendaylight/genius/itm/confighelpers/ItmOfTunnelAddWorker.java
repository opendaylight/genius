/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.genius.datastoreutils.listeners.DataTreeEventCallbackRegistrar;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.impl.ITMBatchingUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeVxlanGpe;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.OvsBridgeRefInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTepKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.OptionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmOfTunnelAddWorker {

    private static final Logger LOG = LoggerFactory.getLogger(ItmOfTunnelAddWorker.class) ;
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");

    private final DataBroker dataBroker;
    private final ManagedNewTransactionRunner txRunner;
    private final JobCoordinator jobCoordinator;
    private final DirectTunnelUtils directTunnelUtils;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;
    private final DataTreeEventCallbackRegistrar eventCallbacks;
    private final ItmConfig itmCfg;

    public ItmOfTunnelAddWorker(DataBroker dataBroker, JobCoordinator jobCoordinator, ItmConfig itmCfg,
                                DirectTunnelUtils directTunnelUtils, OvsBridgeRefEntryCache ovsBridgeRefEntryCache,
                                DataTreeEventCallbackRegistrar eventCallbacks) {
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.jobCoordinator = jobCoordinator;
        this.itmCfg = itmCfg;
        this.directTunnelUtils = directTunnelUtils;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
        this.eventCallbacks = eventCallbacks;
    }

    public Collection<? extends ListenableFuture<?>> addOfPort(Map<OfDpnTepKey, OfDpnTep> dpnsTepMap) {
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(transaction -> {
            for (OfDpnTep dpn : dpnsTepMap.values()) {
                buildOfPort(dpn, transaction);
            }
            updateOfDpnsConfig(dpnsTepMap, transaction);
        }));
    }

    private void updateOfDpnsConfig(Map<OfDpnTepKey, OfDpnTep> dpnsTepMap, WriteTransaction tx) {
        DpnTepConfigBuilder tepConfigBuilder = new DpnTepConfigBuilder();
        tepConfigBuilder.setOfDpnTep(dpnsTepMap);
        InstanceIdentifier<DpnTepConfig> dpnTepsConfII = InstanceIdentifier.builder(DpnTepConfig.class).build();
        tx.merge(LogicalDatastoreType.CONFIGURATION, dpnTepsConfII, tepConfigBuilder.build());
    }

    private void buildOfPort(OfDpnTep dpnTep, WriteTransaction transaction) throws ReadFailedException {
        Optional<OvsBridgeRefEntry> ovsBridgeRefEntry = ovsBridgeRefEntryCache.get(dpnTep.getSourceDpnId());
        DirectTunnelUtils.createBridgeTunnelEntryInConfigDS(dpnTep.getSourceDpnId(), dpnTep.getOfPortName());

        if (ovsBridgeRefEntry.isPresent()) {
            LOG.debug("creating bridge interface on dpn {}", dpnTep.getSourceDpnId());
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                    (InstanceIdentifier<OvsdbBridgeAugmentation>) ovsBridgeRefEntry.get()
                            .getOvsBridgeReference().getValue();
            LOG.debug("adding port to the bridge:{} tunnelName: {}", bridgeIid, dpnTep.getOfPortName());
            addOfPortToBridge(bridgeIid, dpnTep);
        } else {
            LOG.debug("Bridge not found. Registering Eventcallback for dpid {}", dpnTep.getSourceDpnId());

            InstanceIdentifier<OvsBridgeRefEntry> bridgeRefEntryFromDS =
                    InstanceIdentifier.builder(OvsBridgeRefInfo.class)
                            .child(OvsBridgeRefEntry.class, new OvsBridgeRefEntryKey(dpnTep.getSourceDpnId())).build();

            eventCallbacks.onAdd(LogicalDatastoreType.OPERATIONAL, bridgeRefEntryFromDS, (refEntryIid) -> {
                addPortToBridgeOnCallback(dpnTep, refEntryIid);
                return DataTreeEventCallbackRegistrar.NextAction.UNREGISTER;
            }, Duration.ofMillis(5000), (id) -> {
                    try {
                        Optional<OvsBridgeRefEntry> ovsBridgeRefEntryOnCallback =
                                ovsBridgeRefEntryCache.get(dpnTep.getSourceDpnId());
                        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIidOnCallback =
                                (InstanceIdentifier<OvsdbBridgeAugmentation>) ovsBridgeRefEntryOnCallback.get()
                                        .getOvsBridgeReference().getValue();
                        addOfPortToBridge(bridgeIidOnCallback, dpnTep);
                    } catch (ReadFailedException e) {
                        LOG.error("Bridge not found in DS/cache for dpId {}", dpnTep.getSourceDpnId());
                    }
                });
        }

    }

    private void addPortToBridgeOnCallback(OfDpnTep dpnTep, OvsBridgeRefEntry bridgeRefEntry) {
        InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                (InstanceIdentifier<OvsdbBridgeAugmentation>) bridgeRefEntry.getOvsBridgeReference().getValue();
        addOfPortToBridge(bridgeIid, dpnTep);
    }

    private void addOfPortToBridge(InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid, OfDpnTep dpnTep) {
        Class<? extends InterfaceTypeBase> type =
                DirectTunnelUtils.TUNNEL_TYPE_MAP.get(dpnTep.getTunnelType());
        if (type == null) {
            LOG.warn("Unknown Tunnel Type obtained while creating port {} on dpn {}",
                    dpnTep.getOfPortName(), dpnTep.getSourceDpnId());
            return;
        }

        ImmutableMap.Builder<String, String> options = new ImmutableMap.Builder<>();

        // Options common to any kind of tunnel
        options.put(DirectTunnelUtils.TUNNEL_OPTIONS_KEY, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
        IpAddress localIp = dpnTep.getTepIp();
        options.put(DirectTunnelUtils.TUNNEL_OPTIONS_LOCAL_IP, localIp.stringValue());
        options.put(DirectTunnelUtils.TUNNEL_OPTIONS_REMOTE_IP, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
        options.put(DirectTunnelUtils.TUNNEL_OPTIONS_TOS, DirectTunnelUtils.TUNNEL_OPTIONS_TOS_VALUE_INHERIT);

        if (TunnelTypeVxlanGpe.class.equals(type)) {
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_EXTS, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_GPE);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSI, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSP, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSHC1, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSHC2, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSHC3, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_NSHC4, DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_FLOW);
            // VxLAN-GPE interfaces will not use the default UDP port to avoid problems with other meshes
            options.put(DirectTunnelUtils.TUNNEL_OPTIONS_DESTINATION_PORT,
                    DirectTunnelUtils.TUNNEL_OPTIONS_VALUE_GPE_DESTINATION_PORT);
        }

        addTerminationPoint(bridgeIid, dpnTep.getOfPortName(), 0, type, options.build());
    }

    private void addTerminationPoint(InstanceIdentifier<?> bridgeIid, String ofPortName, int vlanId,
                                     Class<? extends InterfaceTypeBase> type, Map<String, String> options) {

        final InstanceIdentifier<TerminationPoint> tpIid = DirectTunnelUtils.createTerminationPointInstanceIdentifier(
                InstanceIdentifier.keyOf(bridgeIid.firstIdentifierOf(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang
                                .network.topology.rev131021.network.topology.topology.Node.class)), ofPortName);
        OvsdbTerminationPointAugmentationBuilder tpAugmentationBuilder = new OvsdbTerminationPointAugmentationBuilder();

        tpAugmentationBuilder.setName(ofPortName);

        if (type != null) {
            tpAugmentationBuilder.setInterfaceType(type);
        }

        if (options != null) {
            Map<OptionsKey, Options> optionsMap = new HashMap<>();
            for (Map.Entry<String, String> entry : options.entrySet()) {
                OptionsBuilder optionsBuilder = new OptionsBuilder();
                optionsBuilder.withKey(new OptionsKey(entry.getKey()));
                optionsBuilder.setOption(entry.getKey());
                optionsBuilder.setValue(entry.getValue());
                optionsMap.put(optionsBuilder.key(),optionsBuilder.build());
            }
            tpAugmentationBuilder.setOptions(optionsMap);
        }

        if (vlanId != 0) {
            tpAugmentationBuilder.setVlanMode(OvsdbPortInterfaceAttributes.VlanMode.Access);
            tpAugmentationBuilder.setVlanTag(new VlanId(Uint16.valueOf(vlanId)));
        }

        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.withKey(InstanceIdentifier.keyOf(tpIid));
        tpBuilder.addAugmentation(tpAugmentationBuilder.build());

        ITMBatchingUtils.write(tpIid, tpBuilder.build(), ITMBatchingUtils.EntityType.TOPOLOGY_CONFIG);
    }
}
