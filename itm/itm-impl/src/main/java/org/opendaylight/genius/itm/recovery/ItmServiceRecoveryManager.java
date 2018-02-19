package org.opendaylight.genius.itm.recovery;

import javax.inject.*;
import org.opendaylight.genius.itm.recovery.registry.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.srm.types.rev170711.*;

@Singleton
public class ItmServiceRecoveryManager {

    private final ItmServiceRecoveryRegistry itmServiceRecoveryRegistry;

    @Inject
    public ItmServiceRecoveryManager(ItmServiceRecoveryRegistry itmServiceRecoveryRegistry) {
        this.itmServiceRecoveryRegistry = itmServiceRecoveryRegistry;
    }

    private String getServiceRegistryKey(Class<? extends EntityNameBase> entityName) {
        return entityName.toString();
    }

    /**
     * Initiates recovery mechanism for a particular interface-manager entity.
     * This method tries to check whether there is a registered handler for the incoming
     * service recovery request within interface-manager and redirects the call
     * to the respective handler if found.
     *  @param entityType
     *            The type of service recovery. eg :SERVICE or INSTANCE.
     * @param entityName
     *            The type entity for which recovery has to be started. eg : INTERFACE or DPN.
     * @param entityId
     *            The unique id to represent the entity to be recovered
     */
    public void recoverService(Class<? extends EntityTypeBase> entityType,
                               Class<? extends EntityNameBase> entityName, String entityId) {
        String serviceRegistryKey = getServiceRegistryKey(entityName);
        itmServiceRecoveryRegistry.getRegisteredServiceRecoveryHandler(serviceRegistryKey).recover(entityId);
    }

}
