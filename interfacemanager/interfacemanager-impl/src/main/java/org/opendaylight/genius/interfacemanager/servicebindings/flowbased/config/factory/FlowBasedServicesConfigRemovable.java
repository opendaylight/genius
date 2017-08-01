/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.genius.interfacemanager.servicebindings.flowbased.config.factory;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.bound.services.state.list.BoundServicesState;
import org.opendaylight.yang.gen.v1.urn.opendaylight.genius.interfacemanager.servicebinding.rev160406.service.bindings.services.info.BoundServices;

public interface FlowBasedServicesConfigRemovable {
    void unbindService(List<ListenableFuture<Void>> futures, String interfaceName,BoundServices boundServiceNew,
                       List<BoundServices> boundServicesList, BoundServicesState boundServicesState);
}
