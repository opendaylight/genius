package org.opendaylight.genius.itm.recovery.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.*;
import org.opendaylight.genius.itm.recovery.*;
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
