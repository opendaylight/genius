/*
 * Copyright (c) 2020 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.opendaylight.genius.itm.utils.NodeConnectorInfo;

@Singleton
public class UnprocessedOFNodeConnectorCache {

    private final ConcurrentMap<String, NodeConnectorInfo> unprocessedOFNodeConnectorMap = new ConcurrentHashMap<>();

    public void add(String ofPortName, NodeConnectorInfo nodeConnectorInfo) {
        unprocessedOFNodeConnectorMap.put(ofPortName, nodeConnectorInfo);
    }

    public NodeConnectorInfo get(String ofPortName) {
        return unprocessedOFNodeConnectorMap.get(ofPortName);
    }

    public NodeConnectorInfo remove(String ofPortName) {
        return unprocessedOFNodeConnectorMap.remove(ofPortName);
    }

    public ConcurrentMap<String, NodeConnectorInfo> getAllPresent() {
        return unprocessedOFNodeConnectorMap;
    }
}
