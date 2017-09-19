/*
 * Copyright © 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.testutils;

import org.opendaylight.genius.interfacemanager.globals.InterfaceInfo;

public class TunnelInterfaceDetails {

    private final String srcIp;
    private final String dstIp;
    private final boolean external;
    private final InterfaceInfo interfaceInfo;

    public TunnelInterfaceDetails(String srcIp, String dstIp, boolean external, InterfaceInfo interfaceInfo) {
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.interfaceInfo = interfaceInfo;
        this.external = external;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public String getDstIp() {
        return dstIp;
    }

    public InterfaceInfo getInterfaceInfo() {
        return interfaceInfo;
    }

    boolean isExternal() {
        return external;
    }
}
