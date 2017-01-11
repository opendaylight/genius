/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.model;

import java.math.BigInteger;

public class NBNode {
    private String controller_host_name;
    private BigInteger connected_flow_capable_switches;

    public String getController_host_name() {
        return controller_host_name;
    }

    public void setController_host_name(String controller_host_name) {
        this.controller_host_name = controller_host_name;
    }

    public BigInteger getConnected_flow_capable_switches() {
        return connected_flow_capable_switches;
    }

    public void setConnected_flow_capable_switches(BigInteger connected_flow_capable_switches) {
        this.connected_flow_capable_switches = connected_flow_capable_switches;
    }
}