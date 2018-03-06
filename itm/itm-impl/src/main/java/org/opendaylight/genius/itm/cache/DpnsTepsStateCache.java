/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.interfacemanager.interfaces.IInterfaceManager;
import org.opendaylight.genius.itm.globals.ITMConstants;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.statehelpers.OvsInterfaceStateAddHelper;
import org.opendaylight.genius.itm.itmdirecttunnels.renderer.ovs.utilities.DirectTunnelUtils;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfo;
import org.opendaylight.genius.itm.utils.DpnTepInterfaceInfoBuilder;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.infrautils.jobcoordinator.JobCoordinator;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.DpnTepsState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.DpnsTeps;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DpnsTepsStateCache implements ClusteredDataTreeChangeListener<DpnsTeps> {

    private static final Logger LOG = LoggerFactory.getLogger(DpnsTepsStateCache.class);

    private final DataBroker dataBroker;
    private final IdManagerService idManager;
    private final IMdsalApiManager mdsalApiManager;
    private final DPNTEPsInfoCache dpntePsInfoCache;
    private final JobCoordinator coordinator;
    private final DirectTunnelUtils directTunnelUtils;
    private ListenerRegistration<DpnsTepsStateCache> registration;
    private final DataTreeIdentifier<DpnsTeps> treeId =
            new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, getWildcardPath());
    private ConcurrentMap<BigInteger, DpnsTeps> dpnsTepsCache = new ConcurrentHashMap<>();
    private ConcurrentMap<String, DpnTepInterfaceInfo> dpnsTepsInfInfoCache = new ConcurrentHashMap<>();

    @Inject
    @SuppressWarnings("checkstyle:IllegalCatch")
    public DpnsTepsStateCache(final DataBroker dataBroker,final IdManagerService idManager,
                              final IMdsalApiManager mdsalApiManager, final DPNTEPsInfoCache dpntePsInfoCache,
                              final JobCoordinator jobCoordinator, final IInterfaceManager interfaceManager,
                              final DirectTunnelUtils directTunnelUtils) {
        this.dataBroker = dataBroker;
        this.idManager = idManager;
        this.mdsalApiManager = mdsalApiManager;
        this.dpntePsInfoCache = dpntePsInfoCache;
        this.coordinator = jobCoordinator;
        this.directTunnelUtils = directTunnelUtils;
        try {
            LOG.trace("Registering on path: {}", treeId);
            if (interfaceManager.isItmDirectTunnelsEnabled()) {
                LOG.debug("ITM Direct Tunnels is Enabled, hence registering this listener");
                registration = dataBroker.registerDataTreeChangeListener(treeId, DpnsTepsStateCache.this);
            } else {
                LOG.debug("ITM Direct Tunnels is not Enabled, therefore not registering this listener");
            }
        } catch (final Exception e) {
            LOG.warn("DpnsTepsStateCache registration failed", e);
        }
    }

    @PreDestroy
    public void close() throws Exception {
        if (registration != null) {
            registration.close();
        }
    }

    private InstanceIdentifier<DpnsTeps> getWildcardPath() {
        return InstanceIdentifier.create(DpnTepsState.class).child(DpnsTeps.class);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<DpnsTeps>> changes) {
        for (DataTreeModification<DpnsTeps> change : changes) {
            final DataObjectModification<DpnsTeps> mod = change.getRootNode();
            switch (mod.getModificationType()) {
                case DELETE:
                    removeFromDpnTepInterfaceCache(mod.getDataBefore());
                    break;
                case SUBTREE_MODIFIED:
                case WRITE:
                    addDpnsTepsToCache(mod.getDataAfter());
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled modification type " + mod.getModificationType());
            }
        }
    }

    private void addDpnsTepsToCache(DpnsTeps dpnsTeps) {
        dpnsTepsCache.put(dpnsTeps.getKey().getSourceDpnId(), dpnsTeps);
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            final String key  = getKey(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId());
            DpnTepInterfaceInfo value = new DpnTepInterfaceInfoBuilder()
                    .setTunnelName(remoteDpns.getTunnelName())
                    .setGroupId(dpnsTeps.getGroupId())
                    .setIsMonitoringEnabled(remoteDpns.isMonitoringEnabled())
                    .setIsMonitoringEnabled(remoteDpns.isInternal())
                    .setTunnelType(dpnsTeps.getTunnelType()).build();
            dpnsTepsInfInfoCache.put(key, value);
            directTunnelUtils.addTunnelEndPointInfoToCache(remoteDpns.getTunnelName(),
                    dpnsTeps.getSourceDpnId().toString(), remoteDpns.getDestinationDpnId().toString());
            NodeConnectorInfo nodeConnectorInfo =
                    directTunnelUtils.getUnprocessedNodeConnector(remoteDpns.getTunnelName());
            if (nodeConnectorInfo != null) {
                processUnporcessedNodeConnectorForTunnel(nodeConnectorInfo, remoteDpns);
            }
        }
    }

    private void removeFromDpnTepInterfaceCache(DpnsTeps dpnsTeps) {
        for (RemoteDpns remoteDpns : dpnsTeps.getRemoteDpns()) {
            String key = getKey(dpnsTeps.getSourceDpnId(), remoteDpns.getDestinationDpnId());
            dpnsTepsInfInfoCache.remove(key);
        }
    }

    protected String getKey(BigInteger src, BigInteger dst) {
        return src + ":" + dst;
    }

    protected String getKey(String src, String dst) {
        return src + ":" + dst;
    }


    public Collection<DpnsTeps> getAllPresent() {
        return dpnsTepsCache.values();
    }

    public DpnsTeps get(BigInteger srcDpnId) {
        return dpnsTepsCache.get(srcDpnId);
    }

    public DpnTepInterfaceInfo getDpnTepInterfaceInfo(BigInteger srcDpnId, BigInteger dstDpnId) {
        return dpnsTepsInfInfoCache.get(getKey(srcDpnId,dstDpnId));
    }

    public DpnTepInterfaceInfo getDpnTepInterfaceInfo(String srcDpn, String dstDpn) {
        return dpnsTepsInfInfoCache.get(getKey(srcDpn, dstDpn));
    }

    private void processUnporcessedNodeConnectorForTunnel(NodeConnectorInfo nodeConnectorInfo, RemoteDpns
            remoteDpns) {
        LOG.debug("Processing the Unprocessed NodeConnector for Tunnel {}", remoteDpns.getTunnelName());

        String portName = nodeConnectorInfo.getNodeConnector().getName();
        InterfaceStateAddWorkerForUnprocessedNC ifStateAddWorker =
                new InterfaceStateAddWorkerForUnprocessedNC(dataBroker, idManager, mdsalApiManager,
                        nodeConnectorInfo.getNodeConnectorId(), nodeConnectorInfo.getNodeConnector(),
                        portName, dpntePsInfoCache, this, directTunnelUtils);

        coordinator.enqueueJob(portName, ifStateAddWorker, ITMConstants.JOB_MAX_RETRIES);
        // Remove the NodeConnector Entry from Unprocessed Map -- Check if this is the best place to remove ?
        directTunnelUtils.removeNodeConnectorInfoFromCache(remoteDpns.getTunnelName());
    }

    private static class InterfaceStateAddWorkerForUnprocessedNC implements Callable {
        DataBroker dataBroker;
        IdManagerService idManager;
        IMdsalApiManager mdsalApiManager;
        DPNTEPsInfoCache dpntePsInfoCache;
        InstanceIdentifier<FlowCapableNodeConnector> key;
        FlowCapableNodeConnector fcNodeConnectorNew;
        String interfaceName;
        DpnsTepsStateCache dpnTepStateCache;
        DirectTunnelUtils directTunnelUtils;

        InterfaceStateAddWorkerForUnprocessedNC(DataBroker dataBroker,  IdManagerService idManager,
                                                IMdsalApiManager mdsalApiManager,
                                                InstanceIdentifier<FlowCapableNodeConnector> key,
                                                FlowCapableNodeConnector fcNodeConnectorNew,
                                                String portName, DPNTEPsInfoCache dpntePsInfoCache,
                                                DpnsTepsStateCache dpnTepStateCache,
                                                DirectTunnelUtils directTunnelUtils) {
            this.key = key;
            this.fcNodeConnectorNew = fcNodeConnectorNew;
            this.interfaceName = portName;
            this.dataBroker = dataBroker;
            this.idManager = idManager;
            this.mdsalApiManager = mdsalApiManager;
            this.dpntePsInfoCache = dpntePsInfoCache;
            this.dpnTepStateCache = dpnTepStateCache;
            this.directTunnelUtils = directTunnelUtils;
        }

        @Override
        public Object call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsInterfaceStateAddHelper.addState(dataBroker, idManager,
                    mdsalApiManager, key, interfaceName, fcNodeConnectorNew, dpntePsInfoCache, dpnTepStateCache,
                    directTunnelUtils);
        }

        @Override
        public String toString() {
            return "InterfaceStateAddWorker{fcNodeConnectorIdentifier=" + key + ", fcNodeConnectorNew="
                    + fcNodeConnectorNew + ", interfaceName='" + interfaceName + '\'' + '}';
        }
    }
}
