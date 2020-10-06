/*
 * Copyright (c) 2019 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;

@Singleton
public class MigrationInProgressCache {
    @NonNull
    private final ConcurrentMap<String, NodeConnectorId> migrationInProgressCache = new ConcurrentHashMap<>();

    protected void put(@NonNull String interfaceName, NodeConnectorId nodeConnectorId) {
        migrationInProgressCache.put(interfaceName, nodeConnectorId);
    }

    protected void remove(@NonNull String interfaceName) {
        migrationInProgressCache.remove(interfaceName);
    }

    public Optional<NodeConnectorId> get(@NonNull String interfaceName) {
        return Optional.ofNullable(migrationInProgressCache.get(interfaceName));
    }
}