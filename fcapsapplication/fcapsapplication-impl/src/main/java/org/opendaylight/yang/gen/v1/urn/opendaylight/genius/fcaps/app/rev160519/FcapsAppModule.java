package org.opendaylight.yang.gen.v1.urn.opendaylight.genius.fcaps.app.rev160519;

import org.opendaylight.genius.fcapsapp.FcapsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FcapsAppModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.genius.fcaps.app.rev160519.AbstractFcapsAppModule {

    private static final Logger LOG = LoggerFactory.getLogger(FcapsAppModule.class);
    public FcapsAppModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public FcapsAppModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.genius.fcaps.app.rev160519.FcapsAppModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Fcaps module initialization");
        return new FcapsProvider(getDataBrokerDependency(),getNotificationAdapterDependency(),getEntityOwnershipServiceDependency());
    }

}
