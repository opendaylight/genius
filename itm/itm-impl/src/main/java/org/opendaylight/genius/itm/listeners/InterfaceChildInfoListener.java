/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.datastoreutils.AsyncDataTreeChangeListenerBase;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelAggregationHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceChildInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceChildInfoListener extends AsyncDataTreeChangeListenerBase<InterfaceParentEntry,
                                                                                InterfaceChildInfoListener> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceChildInfoListener.class);
    private final DataBroker dataBroker;
    private final ItmTunnelAggregationHelper tunnelAggregationHelper;

    @Inject
    public InterfaceChildInfoListener(final DataBroker dataBroker, final ItmTunnelAggregationHelper tunnelAggregation) {
        super(InterfaceParentEntry.class, InterfaceChildInfoListener.class);
        this.dataBroker = dataBroker;
        this.tunnelAggregationHelper = tunnelAggregation;
    }

    @PostConstruct
    public void start() {
        registerListener(LogicalDatastoreType.CONFIGURATION, this.dataBroker);
        LOG.info("InterfaceChildInfoListener Started");
    }

    @Override
    @PreDestroy
    public void close() {
        LOG.info("InterfaceChildInfoListener Closed");
    }

    @Override
    protected InstanceIdentifier<InterfaceParentEntry> getWildCardPath() {
        return InstanceIdentifier.create(InterfaceChildInfo.class).child(InterfaceParentEntry.class);
    }

    @Override
    protected void remove(InstanceIdentifier<InterfaceParentEntry> key, InterfaceParentEntry data) {
        if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: InterfaceChildInfoListener remove for {}", data.getParentInterface());
            tunnelAggregationHelper.updateLogicalTunnelSelectGroup(data, dataBroker);
        }
    }

    @Override
    protected void update(InstanceIdentifier<InterfaceParentEntry> key, InterfaceParentEntry oldData,
                          InterfaceParentEntry data) {
        if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: InterfaceChildInfoListener update for {}", data.getParentInterface());
            tunnelAggregationHelper.updateLogicalTunnelSelectGroup(data, dataBroker);
        }
    }

    @Override
    protected void add(InstanceIdentifier<InterfaceParentEntry> key, InterfaceParentEntry data) {
        if (tunnelAggregationHelper.isTunnelAggregationEnabled()) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: InterfaceChildInfoListener add for {}", data.getParentInterface());
            tunnelAggregationHelper.updateLogicalTunnelSelectGroup(data, dataBroker);
        }
    }

    @Override
    protected InterfaceChildInfoListener getDataTreeChangeListener() {
        return this;
    }
}
