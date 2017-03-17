/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.genius.itm.listeners.cache;

import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.op.rev160406.external.tunnel.list.ExternalTunnelKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ExternalTunnelCache implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ExternalTunnelCache.class);
    private final ConcurrentHashMap<ExternalTunnelKey,ExternalTunnel> externalTunnelCache= new ConcurrentHashMap<>();

    @Inject
    public ExternalTunnelCache() {
    }

    @Override
    @PreDestroy
    public void close() {
    }

    public void remove(ExternalTunnelKey externalTunnelKey) {
        externalTunnelCache.remove(externalTunnelKey);
    }

    public void add(ExternalTunnel externalTunnel) {
        externalTunnelCache.put(externalTunnel.getKey(), externalTunnel);
    }

    public ExternalTunnel get(ExternalTunnelKey externalTunnelKey) {
        return externalTunnelCache.get(externalTunnelKey);
    }
}
