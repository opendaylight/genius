/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OfEndPointCache {
    private static final Logger LOG = LoggerFactory.getLogger(OfEndPointCache.class);

    private final ConcurrentMap<Uint64, String> ofEndPtMap = new ConcurrentHashMap<>();

    public void add(Uint64 dpnId, String ofTunnelName) {
        ofEndPtMap.put(dpnId, ofTunnelName);
    }

    public String get(Uint64 dpnId) {
        return ofEndPtMap.get(dpnId);
    }

    public String remove(Uint64 dpnId) {
        return ofEndPtMap.remove(dpnId);
    }

    public Set<Uint64> getAll() {
        return ofEndPtMap.keySet();
    }
}