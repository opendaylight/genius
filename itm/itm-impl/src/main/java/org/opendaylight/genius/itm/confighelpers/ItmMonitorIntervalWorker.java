/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.util.Datastore;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunner;
import org.opendaylight.mdsal.binding.util.ManagedNewTransactionRunnerImpl;
import org.opendaylight.mdsal.binding.util.TypedWriteTransaction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmMonitorIntervalWorker implements Callable<List<? extends ListenableFuture<?>>> {
    private static final Logger LOG = LoggerFactory.getLogger(ItmMonitorIntervalWorker.class) ;

    private final DataBroker dataBroker;
    private final String tzone;
    private final Uint16 interval;
    private final ManagedNewTransactionRunner txRunner;

    public ItmMonitorIntervalWorker(String tzone, int interval, DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.tzone = tzone;
        this.interval = Uint16.valueOf(interval);
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        LOG.debug("ItmMonitorIntervalWorker: monitorInterval = {}",interval);
        LOG.trace("ItmMonitorToggleWorker initialized with  tzone {} and Interval {}",tzone,interval);
    }

    @Override
    public List<? extends ListenableFuture<?>> call() {
        LOG.debug("Invoking Tunnel Monitor Worker tzone = {} Interval= {}",tzone,interval);
        return toggleTunnelMonitoring();
    }

    private List<? extends ListenableFuture<?>> toggleTunnelMonitoring() {
        List<ListenableFuture<?>> futures = new ArrayList<>();
        List<String> tunnelList = ItmUtils.getInternalTunnelInterfaces(dataBroker);
        LOG.debug("ItmMonitorIntervalWorker toggleTunnelMonitoring: List of tunnel interfaces: {}" , tunnelList);
        InstanceIdentifier<TunnelMonitorInterval> iid = InstanceIdentifier.create(TunnelMonitorInterval.class);
        TunnelMonitorInterval monitorInterval = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.OPERATIONAL,
            tx -> tx.merge(iid, monitorInterval)));
        futures.add(txRunner.callWithNewWriteOnlyTransactionAndSubmit(Datastore.CONFIGURATION,
            tx -> tunnelList.forEach(tunnel -> toggle(tunnel, tx))));
        return futures;
    }

    private void toggle(String tunnelInterfaceName, TypedWriteTransaction<?> tx) {
        if (tunnelInterfaceName != null) {
            LOG.debug("tunnel {} will have monitor interval {}", tunnelInterfaceName, interval);
            tx.merge(ItmUtils.buildTunnelId(tunnelInterfaceName),
                new IfTunnelBuilder().setMonitorInterval(Uint32.valueOf(interval)).build());
        }
    }
}
