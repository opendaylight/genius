/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.commons;

import org.opendaylight.genius.itm.globals.ITMConstants;

public class OvsdbExternalIdsInfo {
    private String tepIp = null;
    private String tzName = null;
    private String dpnBrName = ITMConstants.DEFAULT_BRIDGE_NAME;

    // get methods
    public String getTepIp() {
        return tepIp;
    }

    public String getTzName() {
        return tzName;
    }

    public String getDpnBrName() { return dpnBrName; }

    // set methods
    public void setTepIp(String tepIp) {
        this.tepIp = tepIp;
    }

    public void setTzName(String tzName) {
        this.tzName = tzName;
    }

    public void setDpnBrName(String dpnBrName) {
        this.dpnBrName = dpnBrName;
    }

    @Override
    public String toString() {
        return "OvsdbExternalIdsInfo  { " +
            "Ovsdb node External Ids Info list TEP parameters: TEP IP: " + tepIp + "  TZ name:" + tzName +
            "  DPN bridge name: " + dpnBrName + " }" ;
    }
}
