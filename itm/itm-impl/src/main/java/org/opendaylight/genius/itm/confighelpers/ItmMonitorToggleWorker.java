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
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelMonitoringTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParams;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.TunnelMonitorParamsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItmMonitorToggleWorker implements Callable<List<ListenableFuture<Void>>> {

    private static final Logger LOG = LoggerFactory.getLogger(ItmMonitorToggleWorker.class);

    private final DataBroker dataBroker;
    private final String tzone;
    private final boolean enabled;
    private final Class<? extends TunnelMonitoringTypeBase> monitorProtocol;

    public ItmMonitorToggleWorker(String tzone, boolean enabled,
            Class<? extends TunnelMonitoringTypeBase> monitorProtocol, DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.tzone = tzone;
        this.enabled = enabled;
        this.monitorProtocol = monitorProtocol;
        LOG.trace("ItmMonitorToggleWorker initialized with  tzone {} and toggleBoolean {}",tzone,enabled);
        LOG.debug("TunnelMonitorToggleWorker with monitor protocol = {} ",monitorProtocol);
    }

    @Override public List<ListenableFuture<Void>> call() {
        LOG.debug("ItmMonitorToggleWorker invoked with tzone = {} enabled {}",tzone,enabled);
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        toggleTunnelMonitoring(transaction);
        return Collections.singletonList(transaction.submit());
    }

    private void toggleTunnelMonitoring(WriteTransaction transaction) {
        List<String> tunnelList = ItmUtils.getInternalTunnelInterfaces(dataBroker);
        LOG.debug("toggleTunnelMonitoring: TunnelList size {}", tunnelList.size());
        InstanceIdentifier<TunnelMonitorParams> iid = InstanceIdentifier.builder(TunnelMonitorParams.class).build();
        TunnelMonitorParams protocolBuilder = new TunnelMonitorParamsBuilder()
                .setEnabled(enabled).setMonitorProtocol(monitorProtocol).build();
        LOG.debug("toggleTunnelMonitoring: Updating Operational DS");
        ItmUtils.asyncUpdate(LogicalDatastoreType.OPERATIONAL,iid, protocolBuilder,
                dataBroker, ItmUtils.DEFAULT_CALLBACK);
        if (tunnelList != null && !tunnelList.isEmpty()) {
            for (String tunnel : tunnelList) {
                toggle(tunnel, transaction);
            }
        }
    }

    private void toggle(String tunnelInterfaceName, WriteTransaction transaction) {
        if (tunnelInterfaceName != null) {
            InstanceIdentifier<IfTunnel> trunkIdentifier = ItmUtils.buildTunnelId(tunnelInterfaceName);
            LOG.debug("TunnelMonitorToggleWorker: tunnelInterfaceName: {}, monitorProtocol = {},  "
                    + "monitorEnable = {} ",tunnelInterfaceName, monitorProtocol, enabled);
            IfTunnel tunnel = new IfTunnelBuilder().setMonitorEnabled(enabled)
                    .setMonitorProtocol(monitorProtocol).build();
            transaction.merge(LogicalDatastoreType.CONFIGURATION, trunkIdentifier, tunnel);
        }
    }
}
