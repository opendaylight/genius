package org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.impl.rev160406;

import org.opendaylight.genius.resourcemanager.ResourceManagerServiceProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.idmanager.rev160406.IdManagerService;

public class ResourceManagerImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.impl.rev160406.AbstractResourceManagerImplModule {
    public ResourceManagerImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ResourceManagerImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.genius.resourcemanager.impl.rev160406.ResourceManagerImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        IdManagerService idManagerService = getRpcRegistryDependency().getRpcService(IdManagerService.class);
        ResourceManagerServiceProvider provider = new ResourceManagerServiceProvider(getRpcRegistryDependency());
        provider.setIdManager(idManagerService);
        provider.setRpcProviderRegistry(getRpcRegistryDependency());
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
