/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.listeners;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.genius.datastoreutils.AsyncClusteredDataTreeChangeListenerBase;
import org.opendaylight.genius.datastoreutils.AsyncDataChangeListenerBase;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.IfmConstants;
import org.opendaylight.genius.interfacemanager.IfmUtil;
import org.opendaylight.genius.interfacemanager.commons.InterfaceManagerCommonUtils;
import org.opendaylight.genius.interfacemanager.renderer.ovs.statehelpers.OvsInterfaceTopologyStateUpdateHelper;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.ovsdb.utils.southbound.utils.SouthboundUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.port._interface.attributes.InterfaceExternalIds;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.InterfaceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.rev160406.ParentRefs;

import java.util.List;
import java.util.concurrent.Callable;

public class TerminationPointStateListener extends AsyncClusteredDataTreeChangeListenerBase<OvsdbTerminationPointAugmentation, TerminationPointStateListener> {
    private static final Logger LOG = LoggerFactory.getLogger(TerminationPointStateListener.class);
    private final DataBroker dataBroker;
    private final MdsalUtils mdsalUtils;
    private final SouthboundUtils southboundUtils;

    public TerminationPointStateListener(DataBroker dataBroker) {
        super(OvsdbTerminationPointAugmentation.class, TerminationPointStateListener.class);
        this.dataBroker = dataBroker;
        this.mdsalUtils = new MdsalUtils(dataBroker);
        this.southboundUtils = new SouthboundUtils(mdsalUtils);
    }

    @Override
    protected InstanceIdentifier<OvsdbTerminationPointAugmentation> getWildCardPath() {
        return InstanceIdentifier.builder(NetworkTopology.class).child(Topology.class)
                .child(Node.class).child(TerminationPoint.class).augmentation(OvsdbTerminationPointAugmentation.class).build();
    }

    @Override
    protected TerminationPointStateListener getDataTreeChangeListener() {
        return TerminationPointStateListener.this;
    }

    @Override
    protected void remove(InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                          OvsdbTerminationPointAugmentation tpOld) {
        LOG.debug("Received remove DataChange Notification for ovsdb termination point {}", tpOld.getName());
        if (tpOld.getInterfaceBfdStatus() != null) {
            LOG.debug("Received termination point removed notification with bfd status values {}", tpOld.getName());
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            RendererStateRemoveWorker rendererStateRemoveWorker = new RendererStateRemoveWorker(tpOld);
            jobCoordinator.enqueueJob(tpOld.getName(), rendererStateRemoveWorker);
        }
    }

    @Override
    protected void update(InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                          OvsdbTerminationPointAugmentation tpOld,
                          OvsdbTerminationPointAugmentation tpNew) {
        LOG.debug("Received Update DataChange Notification for ovsdb termination point {}", tpNew.getName());
        if (tpNew.getInterfaceBfdStatus() != null &&
                !tpNew.getInterfaceBfdStatus().equals(tpOld.getInterfaceBfdStatus())) {
            LOG.trace("Bfd Status changed for ovsdb termination point identifier: {},  old: {}, new: {}.",
                    identifier, tpOld, tpNew);
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            RendererStateUpdateWorker rendererStateAddWorker = new RendererStateUpdateWorker(identifier, tpNew);
            jobCoordinator.enqueueJob(tpNew.getName(), rendererStateAddWorker, IfmConstants.JOB_MAX_RETRIES);
        }

        String oldInterfaceName = SouthboundUtils.getExternalInterfaceIdValue(tpOld);
        String newInterfaceName = SouthboundUtils.getExternalInterfaceIdValue(tpNew);
        if (newInterfaceName != null && (oldInterfaceName == null || !oldInterfaceName.equals(newInterfaceName))) {
            String dpnId = southboundUtils.getDatapathIdFromTerminationPoint(tpNew);
            if (dpnId == null) {
                return;
            }
            String parentRefName = InterfaceManagerCommonUtils.getPortNameForInterfaceState(dpnId, tpNew.getName());
            LOG.debug("Detected update to termination point {} with external ID {}, updating parent ref "
                    + "of that interface ID to this termination point's interface-state name {}",
                    tpNew.getName(), newInterfaceName, parentRefName);
            updateExternalIdInterfaceParentRef(newInterfaceName, parentRefName);
        }
    }

    @Override
    protected void add(InstanceIdentifier<OvsdbTerminationPointAugmentation> identifier,
                       OvsdbTerminationPointAugmentation tpNew) {
        LOG.debug("Received add DataChange Notification for ovsdb termination point {}", tpNew.getName());
        if (tpNew.getInterfaceBfdStatus() != null) {
            LOG.debug("Received termination point added notification with bfd status values {}", tpNew.getName());
            DataStoreJobCoordinator jobCoordinator = DataStoreJobCoordinator.getInstance();
            RendererStateUpdateWorker rendererStateUpdateWorker = new RendererStateUpdateWorker(identifier, tpNew);
            jobCoordinator.enqueueJob(tpNew.getName(), rendererStateUpdateWorker, IfmConstants.JOB_MAX_RETRIES);
        }

        String interfaceName = SouthboundUtils.getExternalInterfaceIdValue(tpNew);
        String dpnId = southboundUtils.getDatapathIdFromTerminationPoint(tpNew);
        if (dpnId == null) {
            return;
        }
        String parentRefName = InterfaceManagerCommonUtils.getPortNameForInterfaceState(dpnId, tpNew.getName());
        LOG.debug("Detected add of termination point {} with external ID {}, "
                + "updating parent ref of that interface ID to this termination point's interface-state"
                + "name {}", tpNew.getName(), interfaceName, parentRefName);
        updateExternalIdInterfaceParentRef(interfaceName, parentRefName);
    }

    @SuppressWarnings("unchecked")
    private void updateExternalIdInterfaceParentRef(String interfaceName, String parentInterface) {
        if (interfaceName == null) {
            return;
        }

        WriteTransaction t = dataBroker.newWriteOnlyTransaction();
        IfmUtil.updateInterfaceParentRef(t, interfaceName, parentInterface);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = t.submit();
        Futures.addCallback(submitFuture, new FutureCallback() {

            @Override
            public void onSuccess(Object result) {
                LOG.debug("Successfully updated parentRefInterface for interfaceName {} with parentRef augmentation pointing to {}",
                        interfaceName, parentInterface);
            }

            @Override
            public void onFailure(Throwable t) {
                // FIXME it seems this failure occurs when there is no pre-existing interface, due to missing
                // mandatory field "type"
                LOG.error("Failed updating parentRefInterface for interfaceName {} with parentRef augmentation pointing to {}, exception {}",
                        interfaceName, parentInterface, t);
            }

        });
    }

    private class RendererStateUpdateWorker implements Callable<List<ListenableFuture<Void>>> {
        InstanceIdentifier<OvsdbTerminationPointAugmentation> instanceIdentifier;
        OvsdbTerminationPointAugmentation terminationPointNew;


        public RendererStateUpdateWorker(InstanceIdentifier<OvsdbTerminationPointAugmentation> instanceIdentifier,
                                         OvsdbTerminationPointAugmentation tpNew) {
            this.instanceIdentifier = instanceIdentifier;
            this.terminationPointNew = tpNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            // If another renderer(for eg : CSS) needs to be supported, check can be performed here
            // to call the respective helpers.
            return OvsInterfaceTopologyStateUpdateHelper.updateTunnelState(dataBroker,
                    terminationPointNew);
        }
    }

    private class RendererStateRemoveWorker implements Callable<List<ListenableFuture<Void>>> {
        OvsdbTerminationPointAugmentation terminationPointOld;


        public RendererStateRemoveWorker(OvsdbTerminationPointAugmentation tpNew) {
            this.terminationPointOld = tpNew;
        }

        @Override
        public List<ListenableFuture<Void>> call() throws Exception {
            LOG.debug("Removing bfd state from cache, if any, for {}", terminationPointOld.getName());
            InterfaceManagerCommonUtils.removeBfdStateFromCache(terminationPointOld.getName());
            return null;
        }
    }
}
