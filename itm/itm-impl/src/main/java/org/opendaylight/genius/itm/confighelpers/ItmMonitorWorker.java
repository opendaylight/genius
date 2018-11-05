/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import static org.opendaylight.genius.itm.impl.ItmUtils.nullToEmpty;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmMonitorWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmMonitorWorker.class);

    private final DataBroker dataBroker;
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
        this.dataBroker = dataBroker;
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
        return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(transaction -> {
            toggleTunnelMonitoring(transaction);
        }));
    }

    private void toggleTunnelMonitoring(WriteTransaction transaction) throws ReadFailedException,
        InterruptedException, ExecutionException {
        updateOperationalDS(transaction);
        Collection<DpnsTeps> dpnsTeps = dpnTepStateCache.getAllPresent();
        LOG.debug("toggleTunnelMonitoring: DpnsTepsList size {}", dpnsTeps.size());
        if (dpnsTeps.isEmpty()) {
            LOG.info("There are no teps configured");
        }
        else {
            for (DpnsTeps dpnTeps : dpnsTeps) {
                toggleForDirectEnabled(dpnTeps, transaction);
            }
        }
    }

    private void updateOperationalDS(WriteTransaction transaction) {
        LOG.debug("toggleTunnelMonitoring: Updating Operational DS");
        if (enabled != null) {
            InstanceIdentifier<TunnelMonitorParams> iid = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
            TunnelMonitorParams monitorBuilder = new TunnelMonitorParamsBuilder()
                .setEnabled(enabled).setMonitorProtocol(monitorProtocol).build();
            LOG.debug("toggleTunnelMonitoring: TunnelMonitorParams {}", monitorBuilder);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, iid, monitorBuilder);
        } else if (interval != null) {
            InstanceIdentifier<TunnelMonitorInterval> iid = InstanceIdentifier.builder(TunnelMonitorInterval.class)
                                                                .build();
            TunnelMonitorInterval intervalBuilder = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
            LOG.debug("updateTunnelMonitorInterval: TunnelMonitorInterval {}", intervalBuilder);
            transaction.merge(LogicalDatastoreType.OPERATIONAL, iid, intervalBuilder);
        }
    }

    private void toggleForDirectEnabled(DpnsTeps dpnTeps, WriteTransaction transaction) throws ReadFailedException {
        List<RemoteDpns> remoteDpnTepNewList = new ArrayList<>();
        RemoteDpns remoteDpnNew = null;
        Optional<OvsBridgeRefEntry> ovsBridgeRefEntry = ovsBridgeRefEntryCache.get(dpnTeps.getSourceDpnId());
        for (RemoteDpns remoteDpn : nullToEmpty(dpnTeps.getRemoteDpns())) {
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
            directTunnelUtils.updateBfdConfiguration(dpnTeps.getSourceDpnId(), remoteDpnNew, ovsBridgeRefEntry);
        }
        updateMonitoringDS(dpnTeps.getSourceDpnId(), remoteDpnTepNewList, transaction);
    }

    public void updateMonitoringDS(BigInteger sourceDpnId,List<RemoteDpns> remoteDpnTepNewList,
                                   WriteTransaction transaction) {
        InstanceIdentifier<DpnsTeps> iid = DirectTunnelUtils.createDpnTepsInstanceIdentifier(sourceDpnId);
        DpnsTepsBuilder builder = new DpnsTepsBuilder().withKey(new DpnsTepsKey(sourceDpnId))
            .setRemoteDpns(remoteDpnTepNewList);
        LOG.debug("DirectTunnelUtils - Builder remoteDPNs: {}", builder.getRemoteDpns());
        transaction.merge(LogicalDatastoreType.CONFIGURATION, iid, builder.build());
    }

}
