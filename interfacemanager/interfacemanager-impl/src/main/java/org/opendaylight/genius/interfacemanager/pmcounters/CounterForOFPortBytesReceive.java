/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.pmcounters;

import java.util.HashMap;
import java.util.Map;

public class CounterForOFPortBytesReceive implements CounterForOFPortBytesReceiveMBean{

    Map<String, Integer> counterCache = new HashMap<>();
    public static Map counterMap = new HashMap<String,String>();
    public void invokePMManagedObjects(Map<String, Integer> map) {
        setCounterDetails(map);
    }

    public Map<String, Integer> getCounterDetails() {
        return counterCache;
    }

    public synchronized void setCounterDetails(Map<String, Integer> map) {
       counterCache = map;
    }

    public Map<String, String> retrieveCounterMap(){
        counterMap = (HashMap) counterCache;
        return counterMap;
    }

}
