/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.commons;

import org.opendaylight.genius.itm.globals.ITMConstants;

public class OvsdbOtherConfigInfo {
    private String localIp = null;

    // get methods
    public String getLocalIp() {
        return localIp;
    }

    // set methods
    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    @Override
    public String toString() {
        return "OvsdbOtherConfigInfo  { "
                + "Ovsdb node Other Configs Info list TEP parameters: Local IP: " + localIp + " }" ;
    }
}
