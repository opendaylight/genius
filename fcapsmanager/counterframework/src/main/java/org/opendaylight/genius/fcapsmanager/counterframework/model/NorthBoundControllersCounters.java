/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.fcapsmanager.counterframework.model;

import java.util.ArrayList;
import java.util.List;

public class NorthBoundControllersCounters {
    private List<NBNode> controller_switch_mappings = new ArrayList<>();

    public List<NBNode> getController_switch_mappings() {
        return controller_switch_mappings;
    }

    public void setController_switch_mappings(List<NBNode> controller_switch_mappings) {
        this.controller_switch_mappings = controller_switch_mappings;
    }
}