package org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.impl.rev160406;

import org.opendaylight.genius.alivenessmonitor.internal.AlivenessMonitorProvider;

public class AlivenessMonitorModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.impl.rev160406.AbstractAlivenessMonitorModule {
    public AlivenessMonitorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public AlivenessMonitorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.genius.alivenessmonitor.impl.rev160406.AlivenessMonitorModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        AlivenessMonitorProvider provider = new AlivenessMonitorProvider(getRpcRegistryDependency());
        provider.setNotificationPublishService(getNotificationPublishServiceDependency());
        provider.setNotificationService(getNotificationServiceDependency());
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
