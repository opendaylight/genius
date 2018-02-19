package org.opendaylight.genius.itm.recovery.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.*;
import org.opendaylight.genius.itm.recovery.ItmServiceRecoveryInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmServiceRecoveryRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ItmServiceRecoveryRegistry.class);

    private final Map<String, ItmServiceRecoveryInterface> serviceRecoveryRegistry = new ConcurrentHashMap<>();

    public void registerServiceRecoveryRegistry(String entityName,
                                                ItmServiceRecoveryInterface itmserviceRecoveryHandler) {
        serviceRecoveryRegistry.put(entityName, itmserviceRecoveryHandler);
        LOG.trace("Registered service recovery handler for {}", entityName);
    }

    public ItmServiceRecoveryInterface getRegisteredServiceRecoveryHandler(String entityName) {
        return serviceRecoveryRegistry.get(entityName);
    }
}
