/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.scaling.renderer.ovs.utilities;

import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class NodeConnectorInfo {
    private final InstanceIdentifier<FlowCapableNodeConnector> nodeConnectorId;
    private final FlowCapableNodeConnector nodeConnector;

    public NodeConnectorInfo(Builder builder) {
        this.nodeConnectorId = builder.nodeConnectorId;
        this.nodeConnector = builder.nodeConnector;
    }

    public InstanceIdentifier<FlowCapableNodeConnector> getNodeConnectorId() {
        return this.nodeConnectorId;
    }

    public FlowCapableNodeConnector getNodeConnector() {
        return  this.nodeConnector;
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

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(NodeConnectorInfo from) {
        return new Builder(from);
    }

    public static final class Builder {
        private InstanceIdentifier<FlowCapableNodeConnector> nodeConnectorId;
        private FlowCapableNodeConnector nodeConnector;

        private Builder() {
        }

        private Builder(NodeConnectorInfo from) {
            this.nodeConnectorId = from.nodeConnectorId;
            this.nodeConnector = from.nodeConnector;
        }

        public Builder nodeConnectorId(InstanceIdentifier<FlowCapableNodeConnector> value) {
            this.nodeConnectorId = value;
            return this;
        }

        public Builder nodeConnector(FlowCapableNodeConnector value) {
            this.nodeConnector = value;
            return this;
        }

        public NodeConnectorInfo build() {
            return new NodeConnectorInfo(this);
        }
    }
}
