/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.pmcounters;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Base class for a counter MBean.
 *
 * @author Thomas Pantelis
 */
abstract class AbstractCounterMBeanImpl {
    private volatile Map<String, Integer> counterCache = new HashMap<>();

    public void invokePMManagedObjects(Map<String, Integer> map) {
        setCounterDetails(map);
    }

    public Map<String, Integer> getCounterDetails() {
        return counterCache;
    }

    public void setCounterDetails(Map<String, Integer> map) {
        counterCache = map;
    }

    public Map<String, String> retrieveCounterMap() {
        Map<String, String> returnMap = new HashMap<>();
        for (Entry<String, Integer> entry : counterCache.entrySet()) {
            returnMap.put(entry.getKey(), entry.getValue().toString());
        }
        return returnMap;
    }
}
