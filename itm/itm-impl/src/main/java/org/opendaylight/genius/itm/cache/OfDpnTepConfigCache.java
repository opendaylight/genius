/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.genius.infra.ManagedNewTransactionRunner;
import org.opendaylight.genius.infra.ManagedNewTransactionRunnerImpl;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.OfPortStateAddWorker;
import org.opendaylight.genius.itm.itmdirecttunnels.workers.OfPortStateAddWorkerForNodeConnector;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.infrautils.utils.concurrent.NamedSimpleReentrantLock.Acquired;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTep;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.tep.config.OfDpnTepKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OfDpnTepConfigCache extends DataObjectCache<BigInteger, OfDpnTep> {

    private static final Logger LOG = LoggerFactory.getLogger(DpnTepStateCache.class);
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("GeniusEventLogger");
    private final DirectTunnelUtils directTunnelUtils;
    private final UnprocessedOFNodeConnectorCache unprocessedOFNCCache;
    private final ManagedNewTransactionRunner txRunner;
    private final JobCoordinator coordinator;

    @Inject
    public OfDpnTepConfigCache(DataBroker dataBroker, JobCoordinator coordinator,
                               CacheProvider cacheProvider,
                               DirectTunnelUtils directTunnelUtils,
                               UnprocessedOFNodeConnectorCache unprocessedOFNCCache) {
        super(OfDpnTep.class, dataBroker, LogicalDatastoreType.CONFIGURATION,
            InstanceIdentifier.builder(DpnTepConfig.class).child(OfDpnTep.class).build(), cacheProvider,
            (iid, dpnsTeps) -> dpnsTeps.getSourceDpnId().toJava(),
            sourceDpnId -> InstanceIdentifier.builder(DpnTepConfig.class)
                    .child(OfDpnTep.class, new OfDpnTepKey(Uint64.valueOf(sourceDpnId))).build());
        this.directTunnelUtils = directTunnelUtils;
        this.unprocessedOFNCCache = unprocessedOFNCCache;
        this.coordinator = coordinator;
        this.txRunner = new ManagedNewTransactionRunnerImpl(dataBroker);
    }

    @Override
    protected void added(InstanceIdentifier<OfDpnTep> path, OfDpnTep ofDpnTep) {
        NodeConnectorInfo nodeConnectorInfo = null;
        try (Acquired lock = directTunnelUtils.lockTunnel(ofDpnTep.getOfPortName())) {
            if (unprocessedOFNCCache.get(ofDpnTep.getOfPortName()) != null) {
                nodeConnectorInfo = unprocessedOFNCCache.remove(ofDpnTep.getOfPortName());
            }
        }

        if (nodeConnectorInfo != null && directTunnelUtils.isEntityOwner()) {

            OfPortStateAddWorkerForNodeConnector ifOfStateAddWorker =
                    new OfPortStateAddWorkerForNodeConnector(new OfPortStateAddWorker(directTunnelUtils,
                            ofDpnTep, txRunner), nodeConnectorInfo);
            LOG.debug("ITM-Of-tepInventoryState Entity Owner,ADD {} {}",
                    ofDpnTep.getSourceDpnId(), ofDpnTep.getOfPortName());
            EVENT_LOGGER.debug("ITM-Of-tepInventoryState Entity Owner,ADD {} {}",
                    ofDpnTep.getSourceDpnId(), ofDpnTep.getOfPortName());
            coordinator.enqueueJob(ofDpnTep.getOfPortName(), ifOfStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
        }
    }
}
