/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.scaling.renderer.ovs.statehelpers.OvsInterfaceStateAddHelper;
import org.opendaylight.genius.itm.scaling.renderer.ovs.utilities.ItmScaleUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfoBuilder;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.mdsalutil.cache.DataObjectCache;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.caches.CacheProvider;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
@Singleton
public class DPNTepStateCache extends DataObjectCache<DpnsTeps> {
    private static final Logger LOG = LoggerFactory.getLogger(DPNTepStateCache.class);

    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final IMdsalApiManager mdsalApiManager;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final JobCoordinator coordinator;
    private final ConcurrentHashMap<BigInteger, DpnsTeps> dpnsTepsMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DpnTepInterfaceInfo> dpnTepInterfaceMap = new ConcurrentHashMap<>();

    @Inject
    public DPNTepStateCache(DataBroker dataBroker, IdManagerService idManager, IMdsalApiManager mdsalApiManager,
                            DPNTEPsInfoCache dpntePsInfoCache, CacheProvider cacheProvider,
                            IInterfaceManager interfaceManager, JobCoordinator jobCoordinator) {
        super(DpnsTeps.class, dataBroker, LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.builder(DpnTepsState.class).child(DpnsTeps.class).build(), cacheProvider,
                interfaceManager.isItmDirectTunnelsEnabled());
        this.dataBroker = dataBroker;
        this.idManager = idManager;
        this.mdsalApiManager = mdsalApiManager;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.coordinator = jobCoordinator ;
    }

    @Override
    protected void added(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        dpnsTepsMap.put(dpnsTeps.getSourceDpnId(), dpnsTeps);
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            final String dpn = dpnsTeps.getSourceDpnId() + ":" + remoteDpns.getDestinationDpnId();
            DpnTepInterfaceInfo value = new DpnTepInterfaceInfoBuilder()
                    .setTunnelName(remoteDpns.getTunnelName())
                    .setGroupId(dpnsTeps.getGroupId())
                    .setIsMonitoringEnabled(remoteDpns.isMonitoringEnabled())
                    .setIsMonitoringEnabled(remoteDpns.isInternal())
                    .setTunnelType(dpnsTeps.getTunnelType()).build();
            dpnTepInterfaceMap.put(dpn, value);
            ItmScaleUtils.addTunnelEndPointInfoToCache(remoteDpns.getTunnelName(),dpnsTeps.getSourceDpnId().toString(),
                    remoteDpns.getDestinationDpnId().toString());
            NodeConnectorInfo nodeConnectorInfo = ItmScaleUtils.getUnprocessedNodeConnector(remoteDpns.getTunnelName());
            if (nodeConnectorInfo != null) {
                processUnporcessedNodeConnectorForTunnel(nodeConnectorInfo, remoteDpns);
            }
        }

    }

    @Override
    protected void removed(InstanceIdentifier<DpnsTeps> path, DpnsTeps dpnsTeps) {
        dpnsTepsMap.remove(dpnsTeps.getSourceDpnId());
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            String key = dpnsTeps.getSourceDpnId().toString() + ":" + remoteDpns.getDestinationDpnId().toString();
            dpnTepInterfaceMap.remove(key);
            ItmScaleUtils.removeFromTunnelEndPointInfoCache(remoteDpns.getTunnelName());
        }
    }

    public List<DpnsTeps> getAllDpnsTeps() {
        List<DpnsTeps> dpnsTeps = new ArrayList<>();
        Collection<DpnsTeps> values = dpnsTepsMap.values();
        for (DpnsTeps value : values) {
            dpnsTeps.add(value);
        }
        return dpnsTeps;
    }

    public DpnsTeps getDpnsTepsFromCache(BigInteger dpnId) {
        return dpnsTepsMap.get(dpnId);
    }

    public DpnTepInterfaceInfo getDpnTepInterfaceFromCache(String srcDpnId, String dstDpnId) {
        return dpnTepInterfaceMap.get(srcDpnId + ":" + dstDpnId);
    }


    private void processUnporcessedNodeConnectorForTunnel(NodeConnectorInfo nodeConnectorInfo, RemoteDpns
            remoteDpns) {
        LOG.debug("Processing the Unprocessed NodeConnector for Tunnel {}", remoteDpns.getTunnelName());

        String portName = nodeConnectorInfo.getNodeConnector().getName();
        InterfaceStateAddWorkerForUnprocessedNC ifStateAddWorker =
                new InterfaceStateAddWorkerForUnprocessedNC(dataBroker, idManager, mdsalApiManager,
                        nodeConnectorInfo.getNodeConnectorId(), nodeConnectorInfo.getNodeConnector(),
                        portName, dpntePsInfoCache, this);
        coordinator.enqueueJob(portName, ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
        // Remove the NodeConnector Entry from Unprocessed Map -- Check if this is the best place to remove ?
        ItmScaleUtils.removeNodeConnectorInfoFromCache(remoteDpns.getTunnelName());
    }

    private static class InterfaceStateAddWorkerForUnprocessedNC implements Callable {
        DataBroker dataBroker;
        IdManagerService idManager;
        IMdsalApiManager mdsalApiManager;
        DPNTEPsInfoCache dpntePsInfoCache;
        InstanceIdentifier<FlowCapableNodeConnector> key;
        FlowCapableNodeConnector fcNodeConnectorNew;
        String interfaceName;
        DPNTepStateCache dpnTepStateCache;

        InterfaceStateAddWorkerForUnprocessedNC(DataBroker dataBroker,  IdManagerService idManager,
                                                IMdsalApiManager mdsalApiManager,
                                                InstanceIdentifier<FlowCapableNodeConnector> key,
                                                FlowCapableNodeConnector fcNodeConnectorNew,
                                                String portName, DPNTEPsInfoCache dpntePsInfoCache,
                                                DPNTepStateCache dpnTepStateCache) {
            this.key = key;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
            this.dataBroker = dataBroker;
            this.idManager = idManager;
            this.mdsalApiManager = mdsalApiManager;
            this.dpntePsInfoCache = dpntePsInfoCache;
            this.dpnTepStateCache = dpnTepStateCache;
        }

        @Override
        public Object call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            List<ListenableFuture<Void>> futures = OvsInterfaceStateAddHelper.addState(dataBroker, idManager,
                    mdsalApiManager, key, interfaceName, fcNodeConnectorNew, dpntePsInfoCache, dpnTepStateCache);
            return futures;
        }

        @Override
        public String toString() {
            return "InterfaceStateAddWorker{fcNodeConnectorIdentifier=" + key + ", fcNodeConnectorNew="
                    + fcNodeConnectorNew + ", interfaceName='" + interfaceName + '\'' + '}';
        }
    }

}