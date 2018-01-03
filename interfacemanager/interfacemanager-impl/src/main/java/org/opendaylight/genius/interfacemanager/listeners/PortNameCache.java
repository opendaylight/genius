/*
 * Copyright (c) 2017, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;

@Singleton
public class PortNameCache {
    private final ConcurrentHashMap<String,String> nodeConnectorIdPortNameMap = new ConcurrentHashMap<>();

    protected void updateCache(String nodeConnectorId, String portName) {
        nodeConnectorIdPortNameMap.put(nodeConnectorId, portName);
    }

    protected void removeFromCache(String nodeConnectorId) {
        nodeConnectorIdPortNameMap.remove(nodeConnectorId);
    }

    public Optional<String> getPortNameFromCache(String nodeConnectorId) {
        return Optional.ofNullable(nodeConnectorIdPortNameMap.get(nodeConnectorId));
    }

    public Map<String,String> getPortNameCache() {
        return Collections.unmodifiableMap(nodeConnectorIdPortNameMap);
    }
}