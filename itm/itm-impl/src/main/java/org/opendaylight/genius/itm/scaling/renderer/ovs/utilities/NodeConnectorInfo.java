/*
 * Copyright (c) 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class NodeConnectorInfo {
    private InstanceIdentifier<FlowCapableNodeConnector> nodeConnectorId;
    private FlowCapableNodeConnector nodeConnector;

    public FlowCapableNodeConnector getNodeConnector() {
        return nodeConnector;
    }

    public NodeConnectorInfo(InstanceIdentifier<FlowCapableNodeConnector> nodeConnectorId,
                             FlowCapableNodeConnector nodeConnector) {
        this.nodeConnectorId = nodeConnectorId;
        this.nodeConnector = nodeConnector;
    }

    public InstanceIdentifier<FlowCapableNodeConnector> getNodeConnectorId() {
        return nodeConnectorId;
    }

    public void setNodeConnectorId(InstanceIdentifier<FlowCapableNodeConnector> nodeConnectorId) {
        this.nodeConnectorId = nodeConnectorId;
    }

    public void setNodeConnector(FlowCapableNodeConnector nodeConnector) {
        this.nodeConnector = nodeConnector;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        NodeConnectorInfo that = (NodeConnectorInfo) obj;

        if (getNodeConnectorId() != null
                ? !getNodeConnectorId().equals(that.getNodeConnectorId()) : that.getNodeConnectorId() != null) {
            return false;
        }
        return getNodeConnector() != null
                ? getNodeConnector().equals(that.getNodeConnector()) : that.getNodeConnector() == null;

    }

    @Override
    public int hashCode() {
        return getNodeConnectorId() != null ? getNodeConnectorId().hashCode() : 0;
    }

}
