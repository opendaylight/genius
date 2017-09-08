/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.confighelpers;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.impl.ItmUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.IfTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorInterval;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorIntervalBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmMonitorIntervalWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmMonitorIntervalWorker.class) ;

    private final DataBroker dataBroker;
    private final String tzone;
    private final Integer interval;

    public ItmMonitorIntervalWorker(String tzone, Integer interval, DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.tzone = tzone;
        this.interval = interval;
        LOG.debug("ItmMonitorIntervalWorker: monitorInterval = {}",interval);
        LOG.trace("ItmMonitorToggleWorker initialized with  tzone {} and Interval {}",tzone,interval);
    }

    @Override
    public List<ListenableFuture<Void>> call() {
        LOG.debug("Invoking Tunnel Monitor Worker tzone = {} Interval= {}",tzone,interval);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        toggleTunnelMonitoring(transaction);
        return Collections.singletonList(transaction.submit());
    }

    private void toggleTunnelMonitoring(WriteTransaction transaction) {
        List<String> tunnelList = ItmUtils.getInternalTunnelInterfaces(dataBroker);
        LOG.debug("ItmMonitorIntervalWorker toggleTunnelMonitoring: List of tunnel interfaces: {}" , tunnelList);
        InstanceIdentifier<TunnelMonitorInterval> iid = InstanceIdentifier.builder(TunnelMonitorInterval.class).build();
        TunnelMonitorInterval intervalBuilder = new TunnelMonitorIntervalBuilder().setInterval(interval).build();
        ItmUtils.asyncUpdate(LogicalDatastoreType.OPERATIONAL,iid, intervalBuilder,
                dataBroker, ItmUtils.DEFAULT_CALLBACK);
        if (tunnelList != null && !tunnelList.isEmpty()) {
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
}
