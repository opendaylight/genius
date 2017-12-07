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
    private String tepIp = null;
    private String tzName = null;
    private String brName = ITMConstants.DEFAULT_BRIDGE_NAME;
    private boolean ofTunnel = false;

    // get methods
    public String getTepIp() {
        return tepIp;
    }

    public String getTzName() {
        return tzName;
    }

    public String getBrName() {
        return brName;
    }

    public boolean getOfTunnel() {
        return ofTunnel;
    }

    // set methods
    public void setTepIp(String tepIp) {
        this.tepIp = tepIp;
    }

    public void setTzName(String tzName) {
        this.tzName = tzName;
    }

    public void setBrName(String brName) {
        this.brName = brName;
    }

    public void setOfTunnel(boolean ofTunnel) {
        this.ofTunnel = ofTunnel;
    }

    @Override
    public String toString() {
        return "OvsdbOtherConfigInfo  { "
                + "Ovsdb node Other Configs Info list TEP parameters: TEP IP: " + tepIp + "  TZ name:" + tzName
                + "  Bridge name: " + brName +  " of-tunnel flag: " + ofTunnel + " }" ;
    }
}
