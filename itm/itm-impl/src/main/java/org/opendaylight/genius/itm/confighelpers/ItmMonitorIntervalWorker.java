/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.cache.BfdStateCache;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmMonitorIntervalWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmMonitorIntervalWorker.class) ;

    private final DataBroker dataBroker;
    private final String tzone;
    private final Integer interval;
    private final BfdStateCache bfdStateCache;
    private final DirectTunnelUtils directTunnelUtils;
    private final DpnTepStateCache dpnTepStateCache;
    private final IInterfaceManager interfaceManager;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;
    private final ManagedNewTransactionRunner txRunner;

    public ItmMonitorIntervalWorker(String tzone, Integer interval, DataBroker dataBroker,
                                    BfdStateCache bfdStateCache,
                                    DirectTunnelUtils directTunnelUtils,
                                    DpnTepStateCache dpnTepStateCache,
                                    IInterfaceManager interfaceManager,
                                    OvsBridgeRefEntryCache ovsBridgeRefEntryCache) {
        this.dataBroker = dataBroker;
        this.tzone = tzone;
        this.interval = interval;
        this.bfdStateCache = bfdStateCache;
        this.directTunnelUtils = directTunnelUtils;
        this.dpnTepStateCache = dpnTepStateCache;
        this.interfaceManager = interfaceManager;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        LOG.debug("ItmMonitorIntervalWorker: monitorInterval = {}",interval);
        LOG.trace("ItmMonitorToggleWorker initialized with  tzone {} and Interval {}",tzone,interval);
    }

    @Override
    public List<ListenableFuture<Void>> call() {
        LOG.debug("Invoking Tunnel Monitor Worker tzone = {} Interval= {}",tzone,interval);
/*        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        toggleTunnelMonitoring(transaction);
        return Collections.singletonList(transaction.submit());*/
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(transaction -> {
            toggleTunnelMonitoring(transaction);
        }));
    }

    private void toggleTunnelMonitoring(WriteTransaction transaction) {
        if (interfaceManager.isItmDirectTunnelsEnabled()) {
            toggleMonitoringInterval(transaction);
        } else {
            List<String> tunnelList = ItmUtils.getInternalTunnelInterfaces(dataBroker);
            LOG.debug("ItmMonitorIntervalWorker toggleTunnelMonitoring: List of tunnel interfaces: {}", tunnelList);
            updateOperationalDS(transaction);
            for (String tunnel : tunnelList) {
                toggle(tunnel, transaction);
            }
        }
    }

    private void toggle(String tunnelInterfaceName, WriteTransaction transaction) {
        if (tunnelInterfaceName != null) {
            LOG.debug("tunnel {} will have monitor interval {}", tunnelInterfaceName, interval);
            InstanceIdentifier<IfTunnel> trunkIdentifier = ItmUtils.buildTunnelId(tunnelInterfaceName);
            IfTunnel tunnel = new IfTunnelBuilder().setMonitorInterval(interval.longValue()).build();
            transaction.merge(LogicalDatastoreType.CONFIGURATION, trunkIdentifier, tunnel);
        }
    }

    private void toggleMonitoringInterval(WriteTransaction transaction) {
        Collection<DpnsTeps> dpnsTeps = dpnTepStateCache.getAllPresent();
        LOG.debug("toggleTunnelMonitoring: DpnsTepsList size {}", dpnsTeps.size());
        updateOperationalDS(transaction);
        if (!dpnsTeps.isEmpty()) {
            for (DpnsTeps dpnTeps : dpnsTeps) {
                toggleForDirectEnabled(dpnTeps, transaction);
            }
        }
    }

    private void updateOperationalDS(WriteTransaction transaction) {
        InstanceIdentifier<TunnelMonitorInterval> iid = InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
        TunnelMonitorInterval intervalBuilder = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
        transaction.merge(LogicalDatastoreType.OPERATIONAL, iid, intervalBuilder);
    }

    private void toggleForDirectEnabled(DpnsTeps dpnTeps, WriteTransaction transaction) {
        if (dpnTeps != null) {
            List<RemoteDpns> remoteDpnTepNewList = new ArrayList<>();
            for (RemoteDpns remoteDpn : dpnTeps.getRemoteDpns()) {
                LOG.debug("TepMonitorToggleWorker: tunnelInterfaceName: {}, monitorInterval = {} ",
                    remoteDpn.getTunnelName(), interval);
                RemoteDpnsBuilder remoteDpnNewBld  = new RemoteDpnsBuilder(remoteDpn);
                RemoteDpns remoteDpnNew = remoteDpnNewBld.setMonitoringInterval(interval).build();
                remoteDpnTepNewList.add(remoteDpnNew);
                // Update the parameters on the switch
                LOG.debug("RemoteDpnNew MonitorInterval: {}", remoteDpnNew.getMonitoringInterval());
                LOG.debug("RemoteDPnNew {}", remoteDpnNew);

                updateConfiguration(dpnTeps.getSourceDpnId(), remoteDpnNew);
            }
            InstanceIdentifier<DpnsTeps> iid = InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class,
                new DpnsTepsKey(dpnTeps.getSourceDpnId())).build();
            DpnsTepsBuilder builder = new DpnsTepsBuilder().setKey(new DpnsTepsKey(dpnTeps.getSourceDpnId()))
                                        .setRemoteDpns(remoteDpnTepNewList);
            LOG.debug("builder remoteDPNs: {}", builder.getRemoteDpns());
            transaction.merge(LogicalDatastoreType.CONFIGURATION, iid, builder.build());
        }
    }

    public List<ListenableFuture<Void>> updateConfiguration(BigInteger srcDpnId,
                                                            RemoteDpns remoteDpn) {
        final WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        Optional<OvsBridgeRefEntry> ovsBridgeRefEntry;
        try {
            ovsBridgeRefEntry = ovsBridgeRefEntryCache.get(srcDpnId);
        } catch (ReadFailedException e) {
            LOG.debug("OVS Bridge for the DPN Id {} is not present", srcDpnId);
            return Collections.emptyList();
        }
        if (ovsBridgeRefEntry.isPresent()) {
            LOG.debug("creating bridge interface on dpn {}", srcDpnId);
            InstanceIdentifier<OvsdbBridgeAugmentation> bridgeIid =
                (InstanceIdentifier<OvsdbBridgeAugmentation>) ovsBridgeRefEntry.get()
                    .getOvsBridgeReference().getValue();
            directTunnelUtils.updateBfdParamtersForTerminationPoint(bridgeIid, remoteDpn);
        }
        return Collections.singletonList(transaction.submit());
    }
}
