package org.opendaylight.yang.gen.v1.urn.opendaylight.genius.mdsalutil.impl.rev160406;

import org.opendaylight.genius.mdsalutil.internal.MDSALUtilProvider;

public class MdsaluttilimplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.genius.mdsalutil.impl.rev160406.AbstractMdsaluttilimplModule {
    public MdsaluttilimplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MdsaluttilimplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.genius.mdsalutil.impl.rev160406.MdsaluttilimplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        MDSALUtilProvider mdsalUtilProvider = new MDSALUtilProvider();
        getBrokerDependency().registerConsumer(mdsalUtilProvider);
        return mdsalUtilProvider ;
    }

}
