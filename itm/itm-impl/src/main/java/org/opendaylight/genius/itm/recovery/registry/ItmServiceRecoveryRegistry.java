/*
 * Copyright (c) 2018 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.itm.recovery.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Singleton;
import org.opendaylight.genius.itm.recovery.ItmServiceRecoveryInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmServiceRecoveryRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ItmServiceRecoveryRegistry.class);

    private final Map<String, ItmServiceRecoveryInterface> itmServiceRecoveryRegistry = new ConcurrentHashMap<>();

    public void registerServiceRecoveryRegistry(String entityName,
                                                ItmServiceRecoveryInterface itmserviceRecoveryHandler) {
        itmServiceRecoveryRegistry.put(entityName, itmserviceRecoveryHandler);
        LOG.trace("Registered service recovery handler for {}", entityName);
    }

    public ItmServiceRecoveryInterface getRegisteredServiceRecoveryHandler(String entityName) {
        return itmServiceRecoveryRegistry.get(entityName);
    }
}
