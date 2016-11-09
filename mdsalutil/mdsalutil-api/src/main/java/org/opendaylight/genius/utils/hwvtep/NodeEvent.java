/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.utils.hwvtep;

import java.io.PrintStream;
import java.util.Objects;

public abstract class NodeEvent extends DebugEvent {

    protected final String nodeId;

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

    @Override
    public int hashCode() {
        return nodeId != null ? nodeId.hashCode() : 0;
    }

    enum NodeStatus {
        Connected,Disconnected
    }

    public static class NodeConnectedEvent extends NodeEvent {

        public NodeConnectedEvent(String nodeId) {
            super(nodeId);
        }

        public void print(PrintStream out) {
            out.print(nodeId);
            out.print(" connected");
        }
    }

    public static class NodeDisconnectedEvent extends NodeEvent {

        public NodeDisconnectedEvent(String nodeId) {
            super(nodeId);
        }

        public void print(PrintStream out) {
            out.print(nodeId);
            out.print(" disconnected");
        }
    }

    public static class ChildAddedEvent extends NodeEvent {

        public ChildAddedEvent(String nodeId) {
            super(nodeId);
        }

        public void print(PrintStream out) {
            out.print(nodeId);
            out.print(" became HA child");
        }
    }
}
