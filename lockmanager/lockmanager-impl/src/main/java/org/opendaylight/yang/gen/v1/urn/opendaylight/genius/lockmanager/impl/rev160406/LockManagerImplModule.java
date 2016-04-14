package org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.impl.rev160406;

import org.opendaylight.lockmanager.LockManagerServiceProvider;

public class LockManagerImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.impl.rev160406.AbstractLockManagerImplModule {
    public LockManagerImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public LockManagerImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.genius.lockmanager.impl.rev160406.LockManagerImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LockManagerServiceProvider provider = new LockManagerServiceProvider(getRpcRegistryDependency());
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
