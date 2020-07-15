/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners;

import static org.opendaylight.genius.infra.Datastore.CONFIGURATION;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.cache.DPNTEPsInfoCache;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelAddWorker;
import org.opendaylight.genius.itm.confighelpers.ItmExternalTunnelDeleteWorker;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.TunnelTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.config.rev160406.ItmConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.endpoints.DPNTEPsInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.DcGatewayIpList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.rev160406.dc.gateway.ip.list.DcGatewayIp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DcGatewayIpListener extends AbstractSyncDataTreeChangeListener<DcGatewayIp> {
    private static final Logger LOG = LoggerFactory.getLogger(DcGatewayIpListener.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");
    private final DPNTEPsInfoCache dpnTEPsInfoCache;
    private final ItmExternalTunnelAddWorker externalTunnelAddWorker;
    private final DataBroker dataBroker;
    private final JobCoordinator coordinator;
    private final ManagedNewTransactionRunner txRunner;

    @Inject
    public DcGatewayIpListener(final DPNTEPsInfoCache dpnTEPsInfoCache, final DataBroker dataBroker,
                               final ItmConfig itmConfig, final JobCoordinator coordinator) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(DcGatewayIpList.class)
                .child(DcGatewayIp.class));
        this.dpnTEPsInfoCache = dpnTEPsInfoCache;
        this.coordinator = coordinator;
        this.dataBroker = dataBroker;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
        this.externalTunnelAddWorker = new ItmExternalTunnelAddWorker(itmConfig, dpnTEPsInfoCache);
    }

    @Override
    public void add(DcGatewayIp input) {
        LOG.debug("Received ADD event for {}", input.getIpAddress());
        coordinator.enqueueJob(input.getIpAddress().stringValue(),
                new DcGatewayIpAddWorker(input.getIpAddress(),input.getTunnnelType()), ITMConstants.JOB_MAX_RETRIES);
    }

    @Override
    public void update(DcGatewayIp original, DcGatewayIp update) {
        //Do nothing
    }

    @Override
    public void remove(DcGatewayIp input) {
        LOG.debug("Received REMOVE event for {}", input.getIpAddress());
        coordinator.enqueueJob(input.getIpAddress().stringValue(),
                new DcGatewayIpRemoveWorker(input.getIpAddress(),input.getTunnnelType()), ITMConstants.JOB_MAX_RETRIES);
    }


    private class DcGatewayIpAddWorker implements Callable<List<? extends ListenableFuture<?>>> {
        private final IpAddress ipAddress;
        private final Class<? extends TunnelTypeBase> tunnelType;

        DcGatewayIpAddWorker(IpAddress ipAddress, Class<? extends TunnelTypeBase> tunnelType) {
            this.ipAddress = ipAddress;
            this.tunnelType = tunnelType;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            EVENT_LOGGER.debug("ITM-DcGatewayIp,ADD {}", ipAddress);
            Collection<DPNTEPsInfo> meshedDpnList = dpnTEPsInfoCache.getAllPresent();
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> externalTunnelAddWorker.buildTunnelsToExternalEndPoint(meshedDpnList,
                        ipAddress, tunnelType, tx)));
        }

        @Override
        public String toString() {
            return "DcGatewayIpAddWorker{ dcGatewayIp =" + ipAddress + '\'' + '}';
        }
    }

    private class DcGatewayIpRemoveWorker implements Callable<List<? extends ListenableFuture<?>>> {
        private final IpAddress ipAddress;
        private final Class<? extends TunnelTypeBase> tunnelType;

        DcGatewayIpRemoveWorker(IpAddress ipAddress, Class<? extends TunnelTypeBase> tunnelType) {
            this.ipAddress = ipAddress;
            this.tunnelType = tunnelType;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            EVENT_LOGGER.debug("ITM-DcGatewayIp,REMOVE {}", ipAddress);
            Collection<DPNTEPsInfo> meshedDpnList = dpnTEPsInfoCache.getAllPresent();
            return Collections.singletonList(txRunner.callWithNewWriteOnlyTransactionAndSubmit(CONFIGURATION,
                tx -> ItmExternalTunnelDeleteWorker.deleteTunnels(meshedDpnList,
                        ipAddress, tunnelType, tx)));
        }

        @Override
        public String toString() {
            return "DcGatewayIpRemoveWorker{ dcGatewayIp =" + ipAddress + '\'' + '}';
        }
    }
}