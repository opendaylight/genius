/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.pmcounters;

import java.util.HashMap;
import java.util.Map;

public class CounterForOFPortBytesSent implements CounterForOFPortBytesSentMBean {

    Map<String, Integer> counterCache = new HashMap<>();
    public static Map counterMap = new HashMap<String, String>();

    @Override
    public void invokePMManagedObjects(Map<String, Integer> map) {
        setCounterDetails(map);
    }

    @Override
    public Map<String, Integer> getCounterDetails() {
        return counterCache;
    }

    @Override
    public synchronized void setCounterDetails(Map<String, Integer> map) {
        counterCache = map;
    }

    @Override
    public Map<String, String> retrieveCounterMap() {
        counterMap = counterCache;
        return counterMap;
    }
}
