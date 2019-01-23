/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OfEndPointCache {

    private static final Logger LOG = LoggerFactory.getLogger(OfEndPointCache.class);

    private final ConcurrentMap<String, String> ofEndPtMap =
            new ConcurrentHashMap<>();

    public void add(String dpnId, String ofTunnelName) {
        ofEndPtMap.put(dpnId, ofTunnelName);
    }

    public String get(String dpnId) {
        return ofEndPtMap.get(dpnId);
    }

    public String remove(String dpnId) {
        return ofEndPtMap.remove(dpnId);
    }

    public Set<String> getAll(String dpnId) {
        return ofEndPtMap.keySet();
    }
}