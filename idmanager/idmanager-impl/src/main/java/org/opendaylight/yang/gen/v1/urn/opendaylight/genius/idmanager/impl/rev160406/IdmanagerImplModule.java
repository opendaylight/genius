package org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.impl.rev160406;

import org.opendaylight.idmanager.IdManagerServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.rev160413.LockManagerService;

public class IdmanagerImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.impl.rev160406.AbstractIdmanagerImplModule {
    public IdmanagerImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public IdmanagerImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.impl.rev160406.IdmanagerImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LockManagerService lockManagerService = getRpcRegistryDependency().getRpcService(LockManagerService.class);
        IdManagerServiceProvider provider = new IdManagerServiceProvider(getRpcRegistryDependency());
        provider.setLockManager(lockManagerService);
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
