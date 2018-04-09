/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.genius.itm.confighelpers.ItmTunnelAggregationHelper;
import org.opendaylight.genius.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406.InterfaceChildInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.meta.rev160406._interface.child.info.InterfaceParentEntry;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class InterfaceChildInfoListener extends AbstractSyncDataTreeChangeListener<InterfaceParentEntry> {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceChildInfoListener.class);

    private final DataBroker dataBroker;
    private final ItmTunnelAggregationHelper tunnelAggregationHelper;

    @Inject
    public InterfaceChildInfoListener(DataBroker dataBroker, ItmTunnelAggregationHelper tunnelAggregation) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION,
              InstanceIdentifier.create(InterfaceChildInfo.class).child(InterfaceParentEntry.class));
        this.dataBroker = dataBroker;
        this.tunnelAggregationHelper = tunnelAggregation;
    }

    @Override
    public void add(@Nonnull InterfaceParentEntry newDataObject) {
        if (ItmTunnelAggregationHelper.isTunnelAggregationEnabled()) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: InterfaceChildInfoListener add for {}",
                      newDataObject.getParentInterface());
            tunnelAggregationHelper.updateLogicalTunnelSelectGroup(newDataObject, dataBroker);
        }
    }

    @Override
    public void remove(@Nonnull InterfaceParentEntry removedDataObject) {
        if (ItmTunnelAggregationHelper.isTunnelAggregationEnabled()) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: InterfaceChildInfoListener remove for {}",
                      removedDataObject.getParentInterface());
            tunnelAggregationHelper.updateLogicalTunnelSelectGroup(removedDataObject, dataBroker);
        }
    }

    @Override
    public void update(@Nonnull InterfaceParentEntry originalDataObject, InterfaceParentEntry updatedDataObject) {
        if (ItmTunnelAggregationHelper.isTunnelAggregationEnabled()) {
            LOG.debug("MULTIPLE_VxLAN_TUNNELS: InterfaceChildInfoListener update for {}",
                      updatedDataObject.getParentInterface());
            tunnelAggregationHelper.updateLogicalTunnelSelectGroup(updatedDataObject, dataBroker);
        }
    }
}
