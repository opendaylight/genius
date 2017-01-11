/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.model;

import java.util.List;

public class NorthBoundSwitchStatistics {
    private List<NBSwitchCounters> flow_capable_switches;

    public List<NBSwitchCounters> getFlow_capable_switches() {
        return flow_capable_switches;
    }

    public void setFlow_capable_switches(List<NBSwitchCounters> flow_capable_switches) {
        this.flow_capable_switches = flow_capable_switches;
    }
}