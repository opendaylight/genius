/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.hwvtep;

import java.util.Objects;

class NodeEvent extends DebugEvent {

    protected String nodeId;

    public NodeEvent(String nodeId) {
        super();
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof NodeEvent) {
            return Objects.equals(nodeId, ((NodeEvent) other).nodeId);
        }
        return false;
    }
}
