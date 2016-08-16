package org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.impl.rev160406;

import org.opendaylight.genius.interfacemanager.InterfacemgrProvider;

public class InterfacemgrImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.impl.rev160406.AbstractInterfacemgrImplModule {
    public InterfacemgrImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public InterfacemgrImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.impl.rev160406.InterfacemgrImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        InterfacemgrProvider provider = new InterfacemgrProvider();
        provider.setRpcProviderRegistry(getRpcRegistryDependency());
        provider.setNotificationService(getNotificationServiceDependency());
        provider.setMdsalManager(getMdsalutilDependency());
        provider.setEntityOwnershipService(getEntityOwnershipServiceDependency());
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
