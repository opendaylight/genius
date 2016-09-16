/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbOtherConfigInfo {

    private static final Logger LOG = LoggerFactory.getLogger(OvsdbOtherConfigInfo.class);
    private String tepIp = "";
    private String tzName = "";
    private String dpnBrName = "";
    private String strDpnId = "";

    // get methods
    public String getTepIp(){
        return tepIp;
    }
    public String getTzName(){
        return tzName;
    }
    public String getDpnBrName(){
        return dpnBrName;
    }
    public String getStrDpnId(){
        return strDpnId;
    }

    // set methods
    public void setTepIp(String tepIp){
        this.tepIp = tepIp;
    }
    public void setTzName(String tzName){
        this.tzName = tzName;
    }
    public void setDpnBrName(String dpnBrName){
        this.dpnBrName = dpnBrName;
    }
    public void setStrDpnId(String strDpnId){
        this.strDpnId = strDpnId;
    }
}
