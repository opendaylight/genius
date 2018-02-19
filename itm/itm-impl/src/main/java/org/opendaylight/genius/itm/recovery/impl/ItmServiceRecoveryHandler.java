package org.opendaylight.genius.itm.recovery.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.genius.itm.recovery.ItmServiceRecoveryInterface;
import org.opendaylight.genius.itm.recovery.listeners.ItmRecoverableListeners;
import org.opendaylight.genius.itm.recovery.registry.ItmServiceRecoveryRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.GeniusItm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ItmServiceRecoveryHandler implements ItmServiceRecoveryInterface {

    private static final Logger LOG = LoggerFactory.getLogger(ItmServiceRecoveryHandler.class);
    private final List<ItmRecoverableListeners> itmRecoverableListeners = Collections.synchronizedList(new ArrayList<>());

    @Inject
    public ItmServiceRecoveryHandler(final ItmServiceRecoveryRegistry itmServiceRecoveryRegistry) {
        LOG.info("registering ITM service recovery handlers");
        itmServiceRecoveryRegistry.registerServiceRecoveryRegistry(buildServiceRegistryKey(), this);
    }

    private String buildServiceRegistryKey() {

        return GeniusItm.class.toString();
    }

    @Override
    public void recoverService(String entityId) {
        LOG.info("recover ITM service by deregistering and registering all relevant listeners");
        try {
            deregisterListeners();
            registerListeners();
        } catch (Exception e) {
            LOG.error("ITM service recovery failed");
            e.printStackTrace();
        }
    }

    private void registerListeners() {
        LOG.info("Re-Registering ITM Listeners for recovery");
        synchronized (itmRecoverableListeners) {
            itmRecoverableListeners.forEach(itmRecoverableListener -> itmRecoverableListener.registerListener());
        }
    }

    private void deregisterListeners() throws Exception {
        LOG.info("De-Registering ITM Listeners for recovery");
        synchronized (itmRecoverableListeners) {
            itmRecoverableListeners.forEach(itmRecoverableListener -> itmRecoverableListener.deregisterListener());
        }
    }
}
