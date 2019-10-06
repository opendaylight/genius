/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.globals;

import java.util.List;
import org.opendaylight.yangtools.yang.common.Uint64;

public class LogicalGroupInterfaceInfo extends InterfaceInfo {
    private static final long serialVersionUID = 1L;
    // List of VXLAN/GRE physical tunnel interfaces makes a logical tunnel
    // interface between a pair of DPNs
    private final List<String> parentInterfaceNames;

    public LogicalGroupInterfaceInfo(String portName, Uint64 srcDpId, List<String> parentInterfaces) {
        super(srcDpId, portName);
        this.parentInterfaceNames = parentInterfaces;
    }

    public List<String> getParentInterfaceNames() {
        return parentInterfaceNames;
    }

    public void addParentInterfaceName(String parentIfname) {
        parentInterfaceNames.add(parentIfname);
    }

    public int getTotalParentInterfaces() {
        return parentInterfaceNames.size();
    }

    public void deleteParentInterfaceName(String parentIfname) {
        parentInterfaceNames.remove(parentIfname);
    }
}
