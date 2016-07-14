package org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.impl.rev160406;

import org.opendaylight.genius.arputil.internal.ArpUtilProvider;

public class ArputilImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.impl.rev160406.AbstractArputilImplModule {
    public ArputilImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ArputilImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.genius.arputil.impl.rev160406.ArputilImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        ArpUtilProvider provider = new ArpUtilProvider(getRpcRegistryDependency(),
                getNotificationPublishServiceDependency(),
                getNotificationServiceDependency(),
                getMdsalutilDependency()
        );
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
