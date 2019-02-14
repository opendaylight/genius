/*
 * Copyright (c) 2017, 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.interfacemanager.listeners;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Singleton;
import org.eclipse.jdt.annotation.NonNull;

@Singleton
public class PortNameCache {
    @NonNull
    private final ConcurrentMap<String,String> nodeConnectorIdPortNameMap = new ConcurrentHashMap<>();

    protected void put(@NonNull String nodeConnectorId, String portName) {
        nodeConnectorIdPortNameMap.put(nodeConnectorId, portName);
    }

    protected void remove(@NonNull String nodeConnectorId) {
        nodeConnectorIdPortNameMap.remove(nodeConnectorId);
    }

    public Optional<String> get(@NonNull String nodeConnectorId) {
        return Optional.ofNullable(nodeConnectorIdPortNameMap.get(nodeConnectorId));
    }
}