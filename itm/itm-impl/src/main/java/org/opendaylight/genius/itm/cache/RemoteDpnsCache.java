/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.dpn.teps.state.dpns.teps.RemoteDpns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class RemoteDpnsCache {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteDpnsCache.class);

    private final ConcurrentMap<BigInteger, List<RemoteDpns>> remoteDpnsMap =
            new ConcurrentHashMap<BigInteger, List<RemoteDpns>>();

    public void add(BigInteger dpnId, List<RemoteDpns> dpnList) {
        remoteDpnsMap.put(dpnId, dpnList);
    }

    public List<RemoteDpns> get(BigInteger dpnId) {
        return remoteDpnsMap.get(dpnId);
    }

    public List<RemoteDpns> remove(BigInteger dpnId) {
        return remoteDpnsMap.remove(dpnId);
    }

    public Set<BigInteger> getAll() {
        return remoteDpnsMap.keySet();
    }

    public boolean containsKey(BigInteger dpnId) {
        return remoteDpnsMap.containsKey(dpnId);
    }
}
