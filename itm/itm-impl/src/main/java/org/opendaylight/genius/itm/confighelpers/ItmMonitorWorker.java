/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import java.util.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.genius.infra.Datastore;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.infra.TypedWriteTransaction;
import org.opendaylight.genius.itm.cache.DpnTepStateCache;
import org.opendaylight.genius.itm.cache.OvsBridgeRefEntryCache;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.meta.rev171210.ovs.bridge.ref.info.OvsBridgeRefEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTepsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpnsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmMonitorWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmMonitorWorker.class);

    private final String tzone;
    private final Class<? extends TunnelMonitoringTypeBase> monitorProtocol;
    private final DirectTunnelUtils directTunnelUtils;
    private final DpnTepStateCache dpnTepStateCache;
    private final OvsBridgeRefEntryCache ovsBridgeRefEntryCache;
    private final ManagedNewTransactionRunner txRunner;
    private final Boolean enabled;
    private final Integer interval;

    public <T> ItmMonitorWorker(String tzone, T monitoring,
                                Class<? extends TunnelMonitoringTypeBase> monitorProtocol, DataBroker dataBroker,
                                DirectTunnelUtils directTunnelUtils,
                                DpnTepStateCache dpnTepStateCache,
                                OvsBridgeRefEntryCache ovsBridgeRefEntryCache) {
        this.tzone = tzone;
        this.monitorProtocol = monitorProtocol;
        this.directTunnelUtils = directTunnelUtils;
        this.dpnTepStateCache = dpnTepStateCache;
        this.ovsBridgeRefEntryCache = ovsBridgeRefEntryCache;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        LOG.trace("ItmMonitorWorker initialized with  tzone {} and toggleBoolean {}", tzone, monitoring);
        if (monitoring instanceof Boolean) {
            this.enabled = (Boolean) monitoring;
            this.interval = null;
        }
        else {
            this.interval = (Integer) monitoring;
            this.enabled = null;
        }
        LOG.debug("Toggle monitoring enabled {} interval {} monitor protocol {}", enabled, interval, monitorProtocol);
    }

    @Override public List<ListenableFuture<Void>> call() {
        LOG.debug("ItmMonitorWorker invoked with tzone = {} enabled {}", tzone, enabled);
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        return toggleTunnelMonitoring(futures);
    }

    private List<ListenableFuture<Void>> toggleTunnelMonitoring(List<ListenableFuture<Void>> futures) {
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.OPERATIONAL,
            tx -> updateOperationalDS(tx)));
        Collection<DpnsTeps> dpnsTepsCollection = dpnTepStateCache.getAllPresent();
        LOG.debug("toggleTunnelMonitoring: DpnsTepsList size {}", dpnsTepsCollection.size());
        if (dpnsTepsCollection.isEmpty()) {
            LOG.info("There are no teps configured");
        }
        else {
            futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
                tx -> {
                    for (DpnsTeps dpnTeps : dpnsTepsCollection) {
                        toggleForDirectEnabled(dpnTeps, tx);
                    }
                }
            ));
        }
        return futures;
    }

    private void updateOperationalDS(TypedWriteTransaction<Datastore.Operational> tx) {
        LOG.debug("toggleTunnelMonitoring: Updating Operational DS");
        if (enabled != null) {
            InstanceIdentifier<TunnelMonitorParams> iid = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
            TunnelMonitorParams monitorBuilder = new TunnelMonitorParamsBuilder()
                .setEnabled(enabled).setMonitorProtocol(monitorProtocol).build();
            LOG.debug("toggleTunnelMonitoring: TunnelMonitorParams {}", monitorBuilder);
            tx.merge(iid, monitorBuilder);
        } else if (interval != null) {
            InstanceIdentifier<TunnelMonitorInterval> iid = InstanceIdentifier.builder(TunnelMonitorInterval.class)
                                                                .build();
            TunnelMonitorInterval intervalBuilder = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
            LOG.debug("updateTunnelMonitorInterval: TunnelMonitorInterval {}", intervalBuilder);
            tx.merge(iid, intervalBuilder);
        }
    }


    private void toggleForDirectEnabled(DpnsTeps dpnTeps, TypedWriteTransaction<Datastore.Configuration> tx)
            throws ReadFailedException, InterruptedException, ExecutionException {
        List<RemoteDpns> remoteDpnTepNewList = new ArrayList<>();
        RemoteDpns remoteDpnNew = null;
        Optional<OvsBridgeRefEntry> ovsBridgeRefEntry = ovsBridgeRefEntryCache.get(dpnTeps.getSourceDpnId());
        for (RemoteDpns remoteDpn : dpnTeps.nonnullRemoteDpns()) {
            if (enabled != null) {
                LOG.debug("toggleMonitoring: tunnelInterfaceName: {}, monitorEnable = {} ",
                    remoteDpn.getTunnelName(), enabled);
                remoteDpnNew = new RemoteDpnsBuilder(remoteDpn).setMonitoringEnabled(enabled).build();
            }
            else if (interval != null) {
                LOG.debug("updateMonitoring: tunnelInterfaceName: {}, interval = {} ",
                    remoteDpn.getTunnelName(), interval);
                remoteDpnNew = new RemoteDpnsBuilder(remoteDpn).setMonitoringInterval(interval).build();
            }
            remoteDpnTepNewList.add(remoteDpnNew);
            LOG.debug("toggleMonitoring: RemoteDpnNew {}", remoteDpnNew);
            directTunnelUtils.updateBfdConfiguration(dpnTeps.getSourceDpnId(),
                                                        remoteDpnNew, ovsBridgeRefEntry);
        }
        updateMonitoringDS(dpnTeps.getSourceDpnId(), remoteDpnTepNewList, tx);
    }

    public void updateMonitoringDS(Uint64 sourceDpnId, List<RemoteDpns> remoteDpnTepNewList,
                                   TypedWriteTransaction<Datastore.Configuration> tx) {
        InstanceIdentifier<DpnsTeps> iid = DirectTunnelUtils.createDpnTepsInstanceIdentifier(sourceDpnId);
        DpnsTepsBuilder builder = new DpnsTepsBuilder().withKey(new DpnsTepsKey(sourceDpnId))
            .setRemoteDpns(remoteDpnTepNewList);
        LOG.debug("DirectTunnelUtils - Builder remoteDPNs: {}", builder.getRemoteDpns());
        tx.merge(iid, builder.build());
    }

}
