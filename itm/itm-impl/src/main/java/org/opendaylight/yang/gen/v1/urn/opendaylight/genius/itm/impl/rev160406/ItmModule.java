package org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.impl.rev160406;

import org.opendaylight.genius.itm.impl.ItmProvider;

public class ItmModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.impl.rev160406.AbstractItmModule {
    public ItmModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ItmModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.genius.itm.impl.rev160406.ItmModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ItmProvider provider = new ItmProvider();
        provider.setMdsalApiManager(getMdsalutilDependency());
        provider.setNotificationPublishService(getNotificationPublishServiceDependency());
        provider.setNotificationService(getNotificationServiceDependency());
        provider.setRpcProviderRegistry(getRpcregistryDependency());
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
