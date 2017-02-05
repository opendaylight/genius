/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.listeners;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.DataStoreJobCoordinator;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateAddable;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.state.factory.FlowBasedServicesStateRendererFactory;
import org.opendaylight.genius.interfacemanager.servicebindings.flowbased.utilities.FlowBasedServicesUtils;
import org.opendaylight.genius.mdsalutil.AbstractDataChangeListener;
import org.opendaylight.genius.mdsalutil.ActionInfo;
import org.opendaylight.genius.mdsalutil.FlowEntity;
import org.opendaylight.genius.mdsalutil.InstructionInfo;
import org.opendaylight.genius.mdsalutil.MDSALUtil;
import org.opendaylight.genius.mdsalutil.MatchInfo;
import org.opendaylight.genius.mdsalutil.NwConstants;
import org.opendaylight.genius.mdsalutil.actions.ActionPuntToController;
import org.opendaylight.genius.mdsalutil.instructions.InstructionApplyActions;
import org.opendaylight.genius.mdsalutil.instructions.InstructionGotoTable;
import org.opendaylight.genius.mdsalutil.interfaces.IMdsalApiManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.ServiceModeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class FlowBasedServicesNodeStateListener extends AbstractDataChangeListener<Node> {

    private static final Logger logger = LoggerFactory.getLogger(FlowBasedServicesNodeStateListener.class);

    private IMdsalApiManager mdsalManager;
    private ListenerRegistration<DataChangeListener> listenerRegistration;
    private final DataBroker broker;

    public FlowBasedServicesNodeStateListener(final DataBroker db, IMdsalApiManager mdsalManager) {
        super(Node.class);
        broker = db;
        this.mdsalManager = mdsalManager;
        registerListener(db);
    }

    private void registerListener(final DataBroker db) {
        try {
            listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL,
                    getWildCardPath(), FlowBasedServicesNodeStateListener.this, AsyncDataBroker.DataChangeScope.SUBTREE);
        } catch (final Exception e) {
            logger.error("IfmNodeConnectorListener: DataChange listener registration fail!", e);
            throw new IllegalStateException("IfmNodeConnectorListener: registration Listener failed.", e);
        }
    }

    private InstanceIdentifier<Node> getWildCardPath() {
        return InstanceIdentifier.create(Nodes.class).child(Node.class);
    }


    @Override
    protected void remove(InstanceIdentifier<Node> identifier, Node del) {

    }

    @Override
    protected void update(InstanceIdentifier<Node> identifier, Node original, Node update) {

    }

    @Override
    protected void add(InstanceIdentifier<Node> identifier, Node add) {
        NodeId nodeId = add.getId();
        String[] node =  nodeId.getValue().split(":");
        if(node.length < 2) {
            logger.warn("Unexpected nodeId {}", nodeId.getValue());
            return;
        }
        BigInteger dpId = new BigInteger(node[1]);
        bindServicesOnTunnelType(dpId);
    }

    private void bindServicesOnTunnelType(BigInteger dpId) {
        logger.debug("Received node add event for {}", dpId);
        DataStoreJobCoordinator coordinator = DataStoreJobCoordinator.getInstance();
        for(Object serviceMode : FlowBasedServicesUtils.SERVICE_MODE_MAP.values()) {
            for(String interfaceName : FlowBasedServicesUtils.tunnelTypeBasedServiceBindingKeywords) {
                FlowBasedServicesStateAddable flowBasedServicesStateAddable = FlowBasedServicesStateRendererFactory.
                        getFlowBasedServicesStateRendererFactory((Class<? extends ServiceModeBase>) serviceMode).
                        getFlowBasedServicesStateAddRenderer();
                RendererStateInterfaceBindWorker stateBindWorker = new RendererStateInterfaceBindWorker(flowBasedServicesStateAddable,
                        interfaceName);
                coordinator.enqueueJob(interfaceName, stateBindWorker);
            }
        }
    }

    private class RendererStateInterfaceBindWorker implements Callable<List<ListenableFuture<Void>>> {
        String iface;
        FlowBasedServicesStateAddable flowBasedServicesStateAddable;

        public RendererStateInterfaceBindWorker(FlowBasedServicesStateAddable flowBasedServicesStateAddable,
                                                String iface) {
            this.flowBasedServicesStateAddable = flowBasedServicesStateAddable;
            this.iface = iface;
        }

        @Override
        public List<ListenableFuture<Void>> call() {
            return flowBasedServicesStateAddable.bindServicesOnInterfaceType(iface);
        }
    }
}
